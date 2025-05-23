/*
 * Copyright (C) 2016-2019 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "DebuggerParseData.h"

#include "Parser.h"

namespace JSC {

void DebuggerPausePositions::forEachBreakpointLocation(int startLine, int startColumn, int endLine, int endColumn, Function<void(const JSTextPosition&)>&& callback)
{
    auto isAfterEnd = [&] (int line, int column) {
        return (line == endLine && column >= endColumn) || line > endLine;
    };

    Vector<JSTextPosition> uniquePositions;
    for (auto it = firstPositionAfter(startLine, startColumn); it != m_positions.end(); ++it) {
        auto line = it->position.line;
        auto column = it->position.column();

        if (isAfterEnd(line, column))
            break;

        if (auto resolvedPosition = breakpointLocationForLineColumn(line, column, it)) {
            if (!isAfterEnd(resolvedPosition->line, resolvedPosition->column()))
                uniquePositions.appendIfNotContains(*resolvedPosition);
        }
    }
    std::sort(uniquePositions.begin(), uniquePositions.end(), [] (const auto& a, const auto& b) {
        if (a.line == b.line)
            return a.column() < b.column();
        return a.line < b.line;
    });
    for (const auto& position : uniquePositions)
        callback(position);
}

DebuggerPausePositions::Positions::iterator DebuggerPausePositions::firstPositionAfter(int line, int column)
{
    DebuggerPausePosition position = { DebuggerPausePositionType::Invalid, JSTextPosition(line, column, 0) };
    return std::lower_bound(m_positions.begin(), m_positions.end(), position, [] (const DebuggerPausePosition& a, const DebuggerPausePosition& b) {
        if (a.position.line == b.position.line)
            return a.position.column() < b.position.column();
        return a.position.line < b.position.line;
    });
}

std::optional<JSTextPosition> DebuggerPausePositions::breakpointLocationForLineColumn(int line, int column)
{
    return breakpointLocationForLineColumn(line, column, firstPositionAfter(line, column));
}

std::optional<JSTextPosition> DebuggerPausePositions::breakpointLocationForLineColumn(int line, int column, DebuggerPausePositions::Positions::iterator it)
{
    if (it == m_positions.end())
        return std::nullopt;

    ASSERT(line <= it->position.line);
    ASSERT(line != it->position.line || column <= it->position.column());

    if (line == it->position.line && column == it->position.column()) {
        // Found an exact position match. Roll forward if this was a function Entry.
        // We are guaranteed to have a Leave for an Entry so we don't need to bounds check.
        while (it->type == DebuggerPausePositionType::Enter)
            ++it;
        return it->position;
    }

    // If the next location is a function Entry we will need to decide if we should go into
    // the function or go past the function. We decide to go into the function if the
    // input is on the same line as the function entry. For example:
    //
    //     1. x;
    //     2.
    //     3. function foo() {
    //     4.     x;
    //     5. }
    //     6.
    //     7. x;
    //
    // If the input was line 2, skip past functions to pause on line 7.
    // If the input was line 3, go into the function to pause on line 4.

    // Valid pause location. Use it.
    auto& firstSlidePosition = *it;
    if (firstSlidePosition.type != DebuggerPausePositionType::Enter)
        return std::optional<JSTextPosition>(firstSlidePosition.position);

    // Determine if we should enter this function or skip past it.
    // If entryStackSize is > 0 we are skipping functions.
    bool shouldEnterFunction = firstSlidePosition.position.line == line;
    int entryStackSize = shouldEnterFunction ? 0 : 1;
    ++it;
    for (; it != m_positions.end(); ++it) {
        auto& slidePosition = *it;
        ASSERT(entryStackSize >= 0);

        // Already skipping functions.
        if (entryStackSize) {
            if (slidePosition.type == DebuggerPausePositionType::Enter)
                entryStackSize++;
            else if (slidePosition.type == DebuggerPausePositionType::Leave)
                entryStackSize--;
            continue;
        }

        // Start skipping functions.
        if (slidePosition.type == DebuggerPausePositionType::Enter) {
            entryStackSize++;
            continue;
        }

        // Found pause position.
        return std::optional<JSTextPosition>(slidePosition.position);
    }

    // No pause positions found.
    return std::nullopt;
}

void DebuggerPausePositions::sort()
{
    std::sort(m_positions.begin(), m_positions.end(), [] (const DebuggerPausePosition& a, const DebuggerPausePosition& b) {
        if (a.position.offset == b.position.offset)
            return a.type < b.type;
        return a.position.offset < b.position.offset;
    });
}

typedef enum { Program, Module } DebuggerParseInfoTag;
template <DebuggerParseInfoTag T> struct DebuggerParseInfo { };

template <> struct DebuggerParseInfo<Program> {
    typedef JSC::ProgramNode RootNode;
    static constexpr LexicallyScopedFeatures lexicallyScopedFeatures = NoLexicallyScopedFeatures;
    static constexpr SourceParseMode parseMode = SourceParseMode::ProgramMode;
    static constexpr JSParserScriptMode scriptMode = JSParserScriptMode::Classic;
};

template <> struct DebuggerParseInfo<Module> {
    typedef JSC::ModuleProgramNode RootNode;
    static constexpr LexicallyScopedFeatures lexicallyScopedFeatures = StrictModeLexicallyScopedFeature;
    static constexpr SourceParseMode parseMode = SourceParseMode::ModuleEvaluateMode;
    static constexpr JSParserScriptMode scriptMode = JSParserScriptMode::Module;
};

template <DebuggerParseInfoTag T>
bool gatherDebuggerParseData(VM& vm, const SourceCode& source, DebuggerParseData& debuggerParseData)
{
    typedef typename DebuggerParseInfo<T>::RootNode RootNode;
    LexicallyScopedFeatures lexicallyScopedFeatures = DebuggerParseInfo<T>::lexicallyScopedFeatures;
    SourceParseMode parseMode = DebuggerParseInfo<T>::parseMode;
    JSParserScriptMode scriptMode = DebuggerParseInfo<T>::scriptMode;

    ParserError error;
    std::unique_ptr<RootNode> rootNode = parseRootNode<RootNode>(vm, source, ImplementationVisibility::Public,
        JSParserBuiltinMode::NotBuiltin, lexicallyScopedFeatures, scriptMode, parseMode,
        error, ConstructorKind::None, nullptr, &debuggerParseData);
    if (!rootNode)
        return false;

    debuggerParseData.pausePositions.sort();

    return true;
}

bool gatherDebuggerParseDataForSource(VM& vm, SourceProvider* provider, DebuggerParseData& debuggerParseData)
{
    ASSERT(provider);
    int startLine = provider->startPosition().m_line.oneBasedInt();
    int startColumn = provider->startPosition().m_column.oneBasedInt();
    SourceCode completeSource(*provider, startLine, startColumn);

    switch (provider->sourceType()) {
    case SourceProviderSourceType::Program:
        return gatherDebuggerParseData<Program>(vm, completeSource, debuggerParseData);
    case SourceProviderSourceType::Module:
        return gatherDebuggerParseData<Module>(vm, completeSource, debuggerParseData);
    default:
        return false;
    }
}

} // namespace JSC
