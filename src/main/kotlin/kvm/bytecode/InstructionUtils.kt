package com.leko.kvm.bytecode

import com.leko.kvm.readableJvm

/**
 * Returns the JVM bytecode opcode represented by this instruction.
 *
 * Instructions that do not have a direct JVM opcode, such as pseudo-instructions
 * used during bytecode generation, return `null`.
 */
val Instruction.opcode: Byte?
    get() = when (this) {
        NopInstruction -> 0
        AConstNullInstruction -> 1
        IConstM1Instruction -> 2
        IConst0Instruction -> 3
        IConst1Instruction -> 4
        IConst2Instruction -> 5
        IConst3Instruction -> 6
        IConst4Instruction -> 7
        IConst5Instruction -> 8
        LConst0Instruction -> 9
        LConst1Instruction -> 10
        FConst0Instruction -> 11
        FConst1Instruction -> 12
        FConst2Instruction -> 13
        DConst0Instruction -> 14
        DConst1Instruction -> 15
        is BipushInstruction -> 16
        is SipushInstruction -> 17
        is LdcInstruction -> 18
        is Ldc2wInstruction -> 20 // LDC2_W

        is ILoadInstruction -> 21
        is LLoadInstruction -> 22
        is FLoadInstruction -> 23
        is DLoadInstruction -> 24
        is ALoadInstruction -> 25
        is IALoadInstruction -> 46
        is LALoadInstruction -> 47
        is FALoadInstruction -> 48
        is DALoadInstruction -> 49
        is AALoadInstruction -> 50
        is BALoadInstruction -> 51
        is CALoadInstruction -> 52
        is SALoadInstruction -> 53

        is IStoreInstruction -> 54
        is LStoreInstruction -> 55
        is FStoreInstruction -> 56
        is DStoreInstruction -> 57
        is AStoreInstruction -> 58
        is IAStoreInstruction -> 79
        is LAStoreInstruction -> 80
        is FAStoreInstruction -> 81
        is DAStoreInstruction -> 82
        is AAStoreInstruction -> 83
        is BAStoreInstruction -> 84
        is CAStoreInstruction -> 85
        is SAStoreInstruction -> 86

        PopInstruction -> 87
        Pop2Instruction -> 88
        DupInstruction -> 89
        DupX1Instruction -> 90
        DupX2Instruction -> 91
        Dup2Instruction -> 92
        Dup2X1Instruction -> 93
        Dup2X2Instruction -> 94
        SwapInstruction -> 95

        IAddInstruction -> 96
        LAddInstruction -> 97
        FAddInstruction -> 98
        DAddInstruction -> 99
        ISubInstruction -> 100
        LSubInstruction -> 101
        FSubInstruction -> 102
        DSubInstruction -> 103
        IMulInstruction -> 104
        LMulInstruction -> 105
        FMulInstruction -> 106
        DMulInstruction -> 107
        IDivInstruction -> 108
        LDivInstruction -> 109
        FDivInstruction -> 110
        DDivInstruction -> 111
        IRemInstruction -> 112
        LRemInstruction -> 113
        FRemInstruction -> 114
        DRemInstruction -> 115
        INegInstruction -> 116
        LNegInstruction -> 117
        FNegInstruction -> 118
        DNegInstruction -> 119
        IShlInstruction -> 120
        LShlInstruction -> 121
        IShrInstruction -> 122
        LShrInstruction -> 123
        IUshrInstruction -> 124
        LUshrInstruction -> 125
        IAndInstruction -> 126
        LAndInstruction -> 127
        IOrInstruction -> 128.toByte()
        LOrInstruction -> 129.toByte()
        IXorInstruction -> 130.toByte()
        LXorInstruction -> 131.toByte()
        is IIncInstruction -> 132.toByte()

        I2LInstruction -> 133.toByte()
        I2FInstruction -> 134.toByte()
        I2DInstruction -> 135.toByte()
        L2IInstruction -> 136.toByte()
        L2FInstruction -> 137.toByte()
        L2DInstruction -> 138.toByte()
        F2IInstruction -> 139.toByte()
        F2LInstruction -> 140.toByte()
        F2DInstruction -> 141.toByte()
        D2IInstruction -> 142.toByte()
        D2LInstruction -> 143.toByte()
        D2FInstruction -> 144.toByte()
        I2BInstruction -> 145.toByte()
        I2CInstruction -> 146.toByte()
        I2SInstruction -> 147.toByte()

        LcmpInstruction -> 148.toByte()
        FcmplInstruction -> 149.toByte()
        FcmpgInstruction -> 150.toByte()
        DcmplInstruction -> 151.toByte()
        DcmpgInstruction -> 152.toByte()

        is IfEqInstruction -> 153.toByte()
        is IfNeInstruction -> 154.toByte()
        is IfLtInstruction -> 155.toByte()
        is IfGeInstruction -> 156.toByte()
        is IfGtInstruction -> 157.toByte()
        is IfLeInstruction -> 158.toByte()

        is IfICmpEqInstruction -> 159.toByte()
        is IfICmpNeInstruction -> 160.toByte()
        is IfICmpLtInstruction -> 161.toByte()
        is IfICmpGeInstruction -> 162.toByte()
        is IfICmpGtInstruction -> 163.toByte()
        is IfICmpLeInstruction -> 164.toByte()

        is IfACmpEqInstruction -> 165.toByte()
        is IfACmpNeInstruction -> 166.toByte()

        is GotoInstruction -> 167.toByte()

        JsrInstruction -> 168.toByte()
        RetInstruction -> 169.toByte()
        is TableSwitchInstruction -> 170.toByte()
        is LookupSwitchInstruction -> 171.toByte()

        IReturnInstruction -> 172.toByte()
        LReturnInstruction -> 173.toByte()
        FReturnInstruction -> 174.toByte()
        DReturnInstruction -> 175.toByte()
        AReturnInstruction -> 176.toByte()
        VReturnInstruction -> 177.toByte()

        is GetStaticInstruction -> 178.toByte()
        is PutStaticInstruction -> 179.toByte()
        is GetFieldInstruction -> 180.toByte()
        is PutFieldInstruction -> 181.toByte()

        is InvokeVirtualInstruction -> 182.toByte()
        is InvokeSpecialInstruction -> 183.toByte()
        is InvokeStaticInstruction -> 184.toByte()
        is InvokeInterfaceInstruction -> 185.toByte()
        is InvokeDynamicInstruction -> 186.toByte()

        is NewInstruction -> 187.toByte()
        is NewArrayInstruction -> 188.toByte()
        is ANewArrayInstruction -> 189.toByte()
        ArrayLengthInstruction -> 190.toByte()
        AThrowInstruction -> 191.toByte()
        is CheckCastInstruction -> 192.toByte()
        is InstanceOfInstruction -> 193.toByte()

        MonitorEnterInstruction -> 194.toByte()
        MonitorExitInstruction -> 195.toByte()

        is MultiANewArrayInstruction -> 197.toByte()

        is IfNullInstruction -> 198.toByte()
        is IfNonNullInstruction -> 199.toByte()

        // pseudo-instructions: no real opcode, they don't emit a byte of their own
        is LabelInstruction -> null
        is LineNumberInstruction -> null
    }

/**
 * Returns the JVM method-handle reference kind associated with this handle kind.
 *
 * The returned value corresponds to the reference kind constants defined by the
 * JVM class-file format.
 */
val HandleKind.opcode: Byte
    get() = when (this) {
        HandleKind.GET_FIELD -> 1
        HandleKind.GET_STATIC -> 2
        HandleKind.PUT_FIELD -> 3
        HandleKind.PUT_STATIC -> 4
        HandleKind.INVOKE_VIRTUAL -> 5
        HandleKind.INVOKE_STATIC -> 6
        HandleKind.INVOKE_SPECIAL -> 7
        HandleKind.NEW_INVOKE_SPECIAL -> 8
        HandleKind.INVOKE_INTERFACE -> 9
    }

/**
 * Produces a human-readable representation of this instruction.
 *
 * The returned string is intended for debugging, logging, bytecode inspection,
 * and other diagnostic output. It represents the instruction and its operands
 * without necessarily matching the textual syntax of a particular assembler.
 */
fun Instruction.readable(): String = when (this) {
    NopInstruction -> "nop"

    // constants
    AConstNullInstruction -> "aconst_null"
    IConstM1Instruction -> "iconst_m1"
    IConst0Instruction -> "iconst_0"
    IConst1Instruction -> "iconst_1"
    IConst2Instruction -> "iconst_2"
    IConst3Instruction -> "iconst_3"
    IConst4Instruction -> "iconst_4"
    IConst5Instruction -> "iconst_5"
    LConst0Instruction -> "lconst_0"
    LConst1Instruction -> "lconst_1"
    FConst0Instruction -> "fconst_0"
    FConst1Instruction -> "fconst_1"
    FConst2Instruction -> "fconst_2"
    DConst0Instruction -> "dconst_0"
    DConst1Instruction -> "dconst_1"
    is BipushInstruction -> "bipush        ${value}"
    is SipushInstruction -> "sipush        ${value}"
    is LdcInstruction -> "ldc           ${literal(value)}"
    is Ldc2wInstruction -> "ldc2_w        ${literal(value)}"

    // loads
    is ILoadInstruction -> "iload_$register".takeIf { register in 0..3 } ?: "iload         $register"
    is LLoadInstruction -> "lload_$register".takeIf { register in 0..3 } ?: "lload         $register"
    is FLoadInstruction -> "fload_$register".takeIf { register in 0..3 } ?: "fload         $register"
    is DLoadInstruction -> "dload_$register".takeIf { register in 0..3 } ?: "dload         $register"
    is ALoadInstruction -> "aload_$register".takeIf { register in 0..3 } ?: "aload         $register"
    is IALoadInstruction -> "iaload"
    is LALoadInstruction -> "laload"
    is FALoadInstruction -> "faload"
    is DALoadInstruction -> "daload"
    is AALoadInstruction -> "aaload"
    is BALoadInstruction -> "baload"
    is CALoadInstruction -> "caload"
    is SALoadInstruction -> "saload"

    // stores
    is IStoreInstruction -> "istore_$register".takeIf { register in 0..3 } ?: "istore        $register"
    is LStoreInstruction -> "lstore_$register".takeIf { register in 0..3 } ?: "lstore        $register"
    is FStoreInstruction -> "fstore_$register".takeIf { register in 0..3 } ?: "fstore        $register"
    is DStoreInstruction -> "dstore_$register".takeIf { register in 0..3 } ?: "dstore        $register"
    is AStoreInstruction -> "astore_$register".takeIf { register in 0..3 } ?: "astore        $register"
    is IAStoreInstruction -> "iastore"
    is LAStoreInstruction -> "lastore"
    is FAStoreInstruction -> "fastore"
    is DAStoreInstruction -> "dastore"
    is AAStoreInstruction -> "aastore"
    is BAStoreInstruction -> "bastore"
    is CAStoreInstruction -> "castore"
    is SAStoreInstruction -> "sastore"

    // stack ops
    PopInstruction -> "pop"
    Pop2Instruction -> "pop2"
    DupInstruction -> "dup"
    DupX1Instruction -> "dup_x1"
    DupX2Instruction -> "dup_x2"
    Dup2Instruction -> "dup2"
    Dup2X1Instruction -> "dup2_x1"
    Dup2X2Instruction -> "dup2_x2"
    SwapInstruction -> "swap"

    // arithmetic
    IAddInstruction -> "iadd"
    LAddInstruction -> "ladd"
    FAddInstruction -> "fadd"
    DAddInstruction -> "dadd"
    ISubInstruction -> "isub"
    LSubInstruction -> "lsub"
    FSubInstruction -> "fsub"
    DSubInstruction -> "dsub"
    IMulInstruction -> "imul"
    LMulInstruction -> "lmul"
    FMulInstruction -> "fmul"
    DMulInstruction -> "dmul"
    IDivInstruction -> "idiv"
    LDivInstruction -> "ldiv"
    FDivInstruction -> "fdiv"
    DDivInstruction -> "ddiv"
    IRemInstruction -> "irem"
    LRemInstruction -> "lrem"
    FRemInstruction -> "frem"
    DRemInstruction -> "drem"
    INegInstruction -> "ineg"
    LNegInstruction -> "lneg"
    FNegInstruction -> "fneg"
    DNegInstruction -> "dneg"
    IShlInstruction -> "ishl"
    LShlInstruction -> "lshl"
    IShrInstruction -> "ishr"
    LShrInstruction -> "lshr"
    IUshrInstruction -> "iushr"
    LUshrInstruction -> "lushr"
    IAndInstruction -> "iand"
    LAndInstruction -> "land"
    IOrInstruction -> "ior"
    LOrInstruction -> "lor"
    IXorInstruction -> "ixor"
    LXorInstruction -> "lxor"
    is IIncInstruction -> "iinc          $index, $amount"

    // conversions
    I2LInstruction -> "i2l"
    I2FInstruction -> "i2f"
    I2DInstruction -> "i2d"
    L2IInstruction -> "l2i"
    L2FInstruction -> "l2f"
    L2DInstruction -> "l2d"
    F2IInstruction -> "f2i"
    F2LInstruction -> "f2l"
    F2DInstruction -> "f2d"
    D2IInstruction -> "d2i"
    D2LInstruction -> "d2l"
    D2FInstruction -> "d2f"
    I2BInstruction -> "i2b"
    I2CInstruction -> "i2c"
    I2SInstruction -> "i2s"

    // comparisons
    LcmpInstruction -> "lcmp"
    FcmplInstruction -> "fcmpl"
    FcmpgInstruction -> "fcmpg"
    DcmplInstruction -> "dcmpl"
    DcmpgInstruction -> "dcmpg"

    // jumps
    is IfEqInstruction -> "ifeq          ${labelRef(target)}"
    is IfNeInstruction -> "ifne          ${labelRef(target)}"
    is IfLtInstruction -> "iflt          ${labelRef(target)}"
    is IfGeInstruction -> "ifge          ${labelRef(target)}"
    is IfGtInstruction -> "ifgt          ${labelRef(target)}"
    is IfLeInstruction -> "ifle          ${labelRef(target)}"
    is IfICmpEqInstruction -> "if_icmpeq     ${labelRef(target)}"
    is IfICmpNeInstruction -> "if_icmpne     ${labelRef(target)}"
    is IfICmpLtInstruction -> "if_icmplt     ${labelRef(target)}"
    is IfICmpGeInstruction -> "if_icmpge     ${labelRef(target)}"
    is IfICmpGtInstruction -> "if_icmpgt     ${labelRef(target)}"
    is IfICmpLeInstruction -> "if_icmple     ${labelRef(target)}"
    is IfACmpEqInstruction -> "if_acmpeq     ${labelRef(target)}"
    is IfACmpNeInstruction -> "if_acmpne     ${labelRef(target)}"
    is IfNullInstruction -> "ifnull        ${labelRef(target)}"
    is IfNonNullInstruction -> "ifnonnull     ${labelRef(target)}"
    is GotoInstruction -> "goto          ${labelRef(target)}"
    JsrInstruction -> "jsr"
    RetInstruction -> "ret"
    is TableSwitchInstruction ->
        "tableswitch   { ${cases.entries.joinToString(", ") { (k, l) -> "$k: ${labelRef(l)}" }}, default: ${labelRef(default)} }"
    is LookupSwitchInstruction ->
        "lookupswitch  { ${cases.entries.joinToString(", ") { (k, l) -> "$k: ${labelRef(l)}" }}, default: ${labelRef(default)} }"

    // returns
    IReturnInstruction -> "ireturn"
    LReturnInstruction -> "lreturn"
    FReturnInstruction -> "freturn"
    DReturnInstruction -> "dreturn"
    AReturnInstruction -> "areturn"
    VReturnInstruction -> "return"

    // fields
    is GetStaticInstruction -> "getstatic     ${field.readableJvm()}"
    is PutStaticInstruction -> "putstatic     ${field.readableJvm()}"
    is GetFieldInstruction -> "getfield      ${field.readableJvm()}"
    is PutFieldInstruction -> "putfield      ${field.readableJvm()}"

    // invocation
    is InvokeVirtualInstruction -> "invokevirtual ${method.readableJvm()}"
    is InvokeSpecialInstruction -> "invokespecial ${method.readableJvm()}"
    is InvokeStaticInstruction -> "invokestatic  ${method.readableJvm()}"
    is InvokeInterfaceInstruction -> "invokeinterface ${method.readableJvm()}"
    is InvokeDynamicInstruction -> "invokedynamic ${bootstrapMethod.owner.jvmName}${bootstrapMethod.descriptor}"

    // object/array/type ops
    is NewInstruction -> "new           $type"
    is NewArrayInstruction -> "newarray      $elementType${if (dimensions > 1) "[$dimensions]" else ""}"
    is ANewArrayInstruction -> "anewarray     $elementType${if (dimensions > 1) "[$dimensions]" else ""}"
    ArrayLengthInstruction -> "arraylength"
    AThrowInstruction -> "athrow"
    is CheckCastInstruction -> "checkcast     $type"
    is InstanceOfInstruction -> "instanceof    $type"
    MonitorEnterInstruction -> "monitorenter"
    MonitorExitInstruction -> "monitorexit"
    is MultiANewArrayInstruction -> "multianewarray $elementType, $dimensions"

    // pseudo-instructions
    is LabelInstruction -> labelRef(label)
    is LineNumberInstruction -> "newline    $line"
}

private fun labelRef(label: Label): String = "L${label.name}:"

private fun literal(value: Any): String = when (value) {
    is String -> "\"$value\""
    else -> value.toString()
}