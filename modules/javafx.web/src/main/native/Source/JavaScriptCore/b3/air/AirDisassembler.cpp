/*
 * Copyright (C) 2017-2023 Apple Inc. All rights reserved.
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
#include "AirDisassembler.h"

#if ENABLE(B3_JIT)

#include "AirBasicBlock.h"
#include "AirCode.h"
#include "AirInst.h"
#include "CCallHelpers.h"
#include "Disassembler.h"
#include "LinkBuffer.h"
#include <wtf/TZoneMallocInlines.h>

namespace JSC { namespace B3 { namespace Air {

WTF_MAKE_TZONE_ALLOCATED_IMPL(Disassembler);

void Disassembler::startEntrypoint(CCallHelpers& jit)
{
    m_entrypointStart = jit.labelIgnoringWatchpoints();
}

void Disassembler::endEntrypoint(CCallHelpers& jit)
{
    m_entrypointEnd = jit.labelIgnoringWatchpoints();
}

void Disassembler::startLatePath(CCallHelpers& jit)
{
    m_latePathStart = jit.labelIgnoringWatchpoints();
}

void Disassembler::endLatePath(CCallHelpers& jit)
{
    m_latePathEnd = jit.labelIgnoringWatchpoints();
}

void Disassembler::startBlock(BasicBlock* block, CCallHelpers& jit)
{
    UNUSED_PARAM(jit);
    m_blocks.append(block);
}

void Disassembler::addInst(Inst* inst, MacroAssembler::Label start, MacroAssembler::Label end)
{
    auto addResult = m_instToRange.add(inst, std::make_pair(start, end));
    RELEASE_ASSERT(addResult.isNewEntry);
}

void Disassembler::dump(Code& code, PrintStream& out, LinkBuffer& linkBuffer, const char* airPrefix, const char* asmPrefix, const ScopedLambda<void(Inst&)>& doToEachInst)
{
    void* codeStart = linkBuffer.entrypoint<DisassemblyPtrTag>().untaggedPtr();
    void* codeEnd = bitwise_cast<uint8_t*>(codeStart) +  linkBuffer.size();

    auto dumpAsmRange = [&] (CCallHelpers::Label startLabel, CCallHelpers::Label endLabel) {
        RELEASE_ASSERT(startLabel.isSet());
        RELEASE_ASSERT(endLabel.isSet());
        CodeLocationLabel<DisassemblyPtrTag> start = linkBuffer.locationOf<DisassemblyPtrTag>(startLabel);
        CodeLocationLabel<DisassemblyPtrTag> end = linkBuffer.locationOf<DisassemblyPtrTag>(endLabel);
        RELEASE_ASSERT(end.dataLocation<uintptr_t>() >= start.dataLocation<uintptr_t>());
        disassemble(start, end.dataLocation<uintptr_t>() - start.dataLocation<uintptr_t>(), codeStart, codeEnd, asmPrefix, out);
    };

    for (BasicBlock* block : m_blocks) {
        block->dumpHeader(out);
        if (code.isEntrypoint(block))
            dumpAsmRange(m_entrypointStart, m_entrypointEnd);

        for (Inst& inst : *block) {
            doToEachInst(inst);

            out.print(airPrefix);
            inst.dump(out);
            out.print("\n");

            auto iter = m_instToRange.find(&inst);
            if (iter == m_instToRange.end()) {
                RELEASE_ASSERT(&inst == &block->last());
                continue;
            }
            auto pair = iter->value;
            dumpAsmRange(pair.first, pair.second);
        }
        block->dumpFooter(out);
    }

    // FIXME: We could be better about various late paths. We can implement
    // this later if we find a strong use for it.
    out.print(tierName, "# Late paths\n");
    dumpAsmRange(m_latePathStart, m_latePathEnd);

    {
        CodeLocationLabel<DisassemblyPtrTag> start = linkBuffer.locationOf<DisassemblyPtrTag>(m_latePathEnd);
        size_t dumpedSize = start.dataLocation<uintptr_t>() - linkBuffer.entrypoint<DisassemblyPtrTag>().dataLocation<uintptr_t>();
        if (dumpedSize < linkBuffer.size()) {
            out.print(tierName, "# Remaining\n");
            disassemble(start, linkBuffer.size() - dumpedSize, codeStart, codeEnd, asmPrefix, out);
        }
    }
}

} } } // namespace JSC::B3::Air

#endif // ENABLE(B3_JIT)
