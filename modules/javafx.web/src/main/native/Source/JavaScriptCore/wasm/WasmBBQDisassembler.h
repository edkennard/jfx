/*
 * Copyright (C) 2023 Apple Inc. All rights reserved.
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

#pragma once

#if ENABLE(WEBASSEMBLY_OMGJIT) || ENABLE(WEBASSEMBLY_BBQJIT)

#include "BytecodeIndex.h"
#include "MacroAssembler.h"
#include "WasmOpcodeOrigin.h"
#include "WasmOps.h"
#include <wtf/TZoneMalloc.h>
#include <wtf/Vector.h>
#include <wtf/text/CString.h>

namespace JSC {

class LinkBuffer;

namespace Wasm {

class BBQCallee;

struct PrefixedOpcode {
    OpType prefixOrOpcode;
    union {
        Ext1OpType ext1Opcode;
        ExtAtomicOpType atomicOpcode;
        ExtSIMDOpType simdOpcode;
        ExtGCOpType gcOpcode;
    } prefixed;

    inline explicit PrefixedOpcode(OpType opcode)
    {
        switch (opcode) {
        default:
            prefixOrOpcode = opcode;
            break;
        case OpType::ExtGC:
        case OpType::Ext1:
        case OpType::ExtAtomic:
        case OpType::ExtSIMD:
            RELEASE_ASSERT_NOT_REACHED();
        }
    }

    inline explicit PrefixedOpcode(OpType prefix, uint32_t opcode)
    {
        prefixOrOpcode = prefix;
        switch (prefix) {
        case OpType::Ext1:
            prefixed.ext1Opcode = static_cast<Ext1OpType>(opcode);
            break;
        case OpType::ExtSIMD:
            prefixed.simdOpcode = static_cast<ExtSIMDOpType>(opcode);
            break;
        case OpType::ExtGC:
            prefixed.gcOpcode = static_cast<ExtGCOpType>(opcode);
            break;
        case OpType::ExtAtomic:
            prefixed.atomicOpcode = static_cast<ExtAtomicOpType>(opcode);
            break;
        default:
            RELEASE_ASSERT_NOT_REACHED_WITH_MESSAGE("Expected a valid WASM opcode prefix.");
        }
    }
};

ASCIILiteral makeString(PrefixedOpcode);

class BBQDisassembler {
    WTF_MAKE_TZONE_ALLOCATED(BBQDisassembler);
public:
    BBQDisassembler();
    ~BBQDisassembler();

    void setStartOfCode(MacroAssembler::Label label) { m_startOfCode = label; }
    void setOpcode(MacroAssembler::Label label, PrefixedOpcode opcode, size_t offset)
    {
        m_labels.append(std::tuple { label, opcode, offset });
    }
    void setEndOfOpcode(MacroAssembler::Label label) { m_endOfOpcode = label; }
    void setEndOfCode(MacroAssembler::Label label) { m_endOfCode = label; }

    void dump(LinkBuffer&);
    void dump(PrintStream&, LinkBuffer&);

private:
    void dumpHeader(PrintStream&, LinkBuffer&);

    struct DumpedOp {
        CString disassembly;
    };
    Vector<DumpedOp> dumpVectorForInstructions(LinkBuffer&, const char* prefix, Vector<std::tuple<MacroAssembler::Label, PrefixedOpcode, size_t>>& labels, MacroAssembler::Label endLabel);

    void dumpForInstructions(PrintStream&, LinkBuffer&, const char* prefix, Vector<std::tuple<MacroAssembler::Label, PrefixedOpcode, size_t>>& labels, MacroAssembler::Label endLabel);
    void dumpDisassembly(PrintStream&, LinkBuffer&, MacroAssembler::Label from, MacroAssembler::Label to);

    MacroAssembler::Label m_startOfCode;
    Vector<std::tuple<MacroAssembler::Label, PrefixedOpcode, size_t>> m_labels;
    MacroAssembler::Label m_endOfOpcode;
    MacroAssembler::Label m_endOfCode;
    void* m_codeStart { nullptr };
    void* m_codeEnd { nullptr };
};

} // namespace Wasm
} // namespace JSC

#endif // ENABLE(WEBASSEMBLY_OMGJIT)
