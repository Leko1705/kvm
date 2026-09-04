package com.leko.kvm.bytecode

import com.leko.kvm.FieldSignature
import com.leko.kvm.MethodSignature
import com.leko.kvm.typing.BooleanType
import com.leko.kvm.typing.ByteType
import com.leko.kvm.typing.CharType
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.DoubleType
import com.leko.kvm.typing.FloatType
import com.leko.kvm.typing.IntType
import com.leko.kvm.typing.LongType
import com.leko.kvm.typing.ReferenceType
import com.leko.kvm.typing.ShortType
import com.leko.kvm.typing.Type
import com.leko.kvm.typing.VoidType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Label @OptIn(ExperimentalUuidApi::class) constructor(val name: String = Uuid.generateV4().toString())

sealed interface Instruction

sealed interface ConstantInstruction : Instruction

data object NopInstruction : Instruction

data object AConstNullInstruction : ConstantInstruction

data object IConstM1Instruction : ConstantInstruction

data object IConst0Instruction : ConstantInstruction

data object IConst1Instruction : ConstantInstruction

data object IConst2Instruction : ConstantInstruction

data object IConst3Instruction : ConstantInstruction

data object IConst4Instruction : ConstantInstruction

data object IConst5Instruction : ConstantInstruction

fun IntConstant(value: Int): ConstantInstruction = when (value) {
    -1 -> IConstM1Instruction
    0 -> IConst0Instruction
    1 -> IConst1Instruction
    2 -> IConst2Instruction
    3 -> IConst3Instruction
    4 -> IConst4Instruction
    5 -> IConst5Instruction
    in Byte.MIN_VALUE..Byte.MAX_VALUE -> BipushInstruction(value.toByte())
    in Short.MIN_VALUE..Short.MAX_VALUE -> SipushInstruction(value.toShort())
    else -> LdcInstruction(value)
}

data object LConst0Instruction : ConstantInstruction

data object LConst1Instruction : ConstantInstruction

fun LongConstant(value: Long) : ConstantInstruction = when (value) {
    0L -> LConst0Instruction
    1L -> LConst1Instruction
    else -> Ldc2wInstruction(value)
}

data object FConst0Instruction : ConstantInstruction

data object FConst1Instruction : ConstantInstruction

data object FConst2Instruction : ConstantInstruction

fun FloatConstant(value: Float) : ConstantInstruction = when (value) {
    0f -> FConst0Instruction
    1f -> FConst1Instruction
    2f -> FConst2Instruction
    else -> LdcInstruction(value)
}

data object DConst0Instruction : ConstantInstruction

data object DConst1Instruction : ConstantInstruction

fun DoubleConstant(value: Double) : ConstantInstruction = when (value) {
    0.0 -> DConst0Instruction
    1.0 -> DConst1Instruction
    else -> Ldc2wInstruction(value)
}

data class BipushInstruction(val value: Byte) : ConstantInstruction

data class SipushInstruction(val value: Short) : ConstantInstruction

data class LdcInstruction(val value: Any) : ConstantInstruction


data class Ldc2wInstruction(val value: Number) : ConstantInstruction {
    init {
        require(value is Double || value is Long) { "value must be either Long or Double" }
    }
}

// ── loads and stores ─────────────────────────────────────────────────────────

sealed class VarLoadInstruction(val register: Int) : Instruction

sealed interface ArrayLoadInstruction : Instruction

class ILoadInstruction(register: Int): VarLoadInstruction(register)

class LLoadInstruction(register: Int): VarLoadInstruction(register)

class FLoadInstruction(register: Int): VarLoadInstruction(register)

class DLoadInstruction(register: Int): VarLoadInstruction(register)

class ALoadInstruction(register: Int): VarLoadInstruction(register)

fun VarLoadInstruction(type: Type, register: Int): VarLoadInstruction = when(type) {
    BooleanType -> ILoadInstruction(register)
    ByteType -> ILoadInstruction(register)
    CharType -> ILoadInstruction(register)
    ShortType -> ILoadInstruction(register)
    IntType -> ILoadInstruction(register)
    LongType -> LLoadInstruction(register)
    FloatType -> FLoadInstruction(register)
    DoubleType -> DLoadInstruction(register)
    VoidType -> error("Can not load local 'void' register")
    is ReferenceType -> ALoadInstruction(register)
}

data object IALoadInstruction: ArrayLoadInstruction

data object LALoadInstruction: ArrayLoadInstruction

data object FALoadInstruction: ArrayLoadInstruction

data object DALoadInstruction: ArrayLoadInstruction

data object AALoadInstruction: ArrayLoadInstruction

data object BALoadInstruction: ArrayLoadInstruction

data object CALoadInstruction: ArrayLoadInstruction

data object SALoadInstruction: ArrayLoadInstruction

fun ArrayLoadInstruction(type: Type): ArrayLoadInstruction = when(type) {
    BooleanType -> IALoadInstruction
    ByteType -> BALoadInstruction
    CharType -> CALoadInstruction
    ShortType -> SALoadInstruction
    IntType -> IALoadInstruction
    LongType -> LALoadInstruction
    FloatType -> FALoadInstruction
    DoubleType -> DALoadInstruction
    VoidType -> error("Can not load array 'void' entry")
    is ReferenceType -> AALoadInstruction
}

sealed class VarStoreInstruction(val register: Int) : Instruction

sealed interface ArrayStoreInstruction : Instruction

class IStoreInstruction(register: Int): VarStoreInstruction(register)

class LStoreInstruction(register: Int): VarStoreInstruction(register)

class FStoreInstruction(register: Int): VarStoreInstruction(register)

class DStoreInstruction(register: Int): VarStoreInstruction(register)

class AStoreInstruction(register: Int): VarStoreInstruction(register)

fun VarStoreInstruction(type: Type, register: Int): VarStoreInstruction = when(type) {
    BooleanType -> IStoreInstruction(register)
    ByteType -> IStoreInstruction(register)
    CharType -> IStoreInstruction(register)
    ShortType -> IStoreInstruction(register)
    IntType -> IStoreInstruction(register)
    LongType -> LStoreInstruction(register)
    FloatType -> FStoreInstruction(register)
    DoubleType -> DStoreInstruction(register)
    VoidType -> error("Can not load local 'void' register")
    is ReferenceType -> AStoreInstruction(register)
}

data object IAStoreInstruction: ArrayStoreInstruction

data object LAStoreInstruction: ArrayStoreInstruction

data object FAStoreInstruction: ArrayStoreInstruction

data object DAStoreInstruction: ArrayStoreInstruction

data object AAStoreInstruction: ArrayStoreInstruction

data object BAStoreInstruction: ArrayStoreInstruction

data object CAStoreInstruction: ArrayStoreInstruction

data object SAStoreInstruction: ArrayStoreInstruction

fun ArrayStoreInstruction(type: Type): ArrayStoreInstruction = when(type) {
    BooleanType -> IAStoreInstruction
    ByteType -> BAStoreInstruction
    CharType -> CAStoreInstruction
    ShortType -> SAStoreInstruction
    IntType -> IAStoreInstruction
    LongType -> LAStoreInstruction
    FloatType -> FAStoreInstruction
    DoubleType -> DAStoreInstruction
    VoidType -> error("Can not load array 'void' entry")
    is ReferenceType -> AAStoreInstruction
}

// ── fields ───────────────────────────────────────────────────────────────────

sealed interface StackOperationInstruction : Instruction

data object PopInstruction: StackOperationInstruction

data object Pop2Instruction: StackOperationInstruction

data object DupInstruction: StackOperationInstruction

data object DupX1Instruction: StackOperationInstruction

data object DupX2Instruction: StackOperationInstruction

data object Dup2Instruction: StackOperationInstruction

data object Dup2X1Instruction: StackOperationInstruction

data object Dup2X2Instruction: StackOperationInstruction

data object SwapInstruction: StackOperationInstruction



// ── arithmetic and stack ──────────────────────────────────────────────────────


sealed interface ArithmeticInstruction: Instruction

data object IAddInstruction: ArithmeticInstruction

data object LAddInstruction: ArithmeticInstruction

data object FAddInstruction: ArithmeticInstruction

data object DAddInstruction: ArithmeticInstruction

data object ISubInstruction: ArithmeticInstruction

data object LSubInstruction: ArithmeticInstruction

data object FSubInstruction: ArithmeticInstruction

data object DSubInstruction: ArithmeticInstruction

data object IMulInstruction: ArithmeticInstruction

data object LMulInstruction: ArithmeticInstruction

data object FMulInstruction: ArithmeticInstruction

data object DMulInstruction: ArithmeticInstruction

data object IDivInstruction: ArithmeticInstruction

data object LDivInstruction: ArithmeticInstruction

data object FDivInstruction: ArithmeticInstruction

data object DDivInstruction: ArithmeticInstruction

data object IRemInstruction: ArithmeticInstruction

data object LRemInstruction: ArithmeticInstruction

data object FRemInstruction: ArithmeticInstruction

data object DRemInstruction: ArithmeticInstruction

data object INegInstruction: ArithmeticInstruction

data object LNegInstruction: ArithmeticInstruction

data object FNegInstruction: ArithmeticInstruction

data object DNegInstruction: ArithmeticInstruction

data object IShlInstruction: ArithmeticInstruction

data object LShlInstruction: ArithmeticInstruction

data object IShrInstruction: ArithmeticInstruction

data object LShrInstruction: ArithmeticInstruction

data object IUshrInstruction: ArithmeticInstruction

data object LUshrInstruction: ArithmeticInstruction

data object IAndInstruction: ArithmeticInstruction

data object LAndInstruction: ArithmeticInstruction

data object IOrInstruction: ArithmeticInstruction

data object LOrInstruction: ArithmeticInstruction

data object IXorInstruction: ArithmeticInstruction

data object LXorInstruction: ArithmeticInstruction

data class IIncInstruction(val index: Int, val amount: Int = 1) : Instruction


data object I2LInstruction: ArithmeticInstruction

data object I2FInstruction: ArithmeticInstruction

data object I2DInstruction: ArithmeticInstruction

data object L2IInstruction: ArithmeticInstruction

data object L2FInstruction: ArithmeticInstruction

data object L2DInstruction: ArithmeticInstruction

data object F2IInstruction: ArithmeticInstruction

data object F2LInstruction: ArithmeticInstruction

data object F2DInstruction: ArithmeticInstruction

data object D2IInstruction: ArithmeticInstruction

data object D2LInstruction: ArithmeticInstruction

data object D2FInstruction: ArithmeticInstruction

data object I2BInstruction: ArithmeticInstruction

data object I2CInstruction: ArithmeticInstruction

data object I2SInstruction: ArithmeticInstruction


data object LcmpInstruction: ArithmeticInstruction

data object FcmplInstruction: ArithmeticInstruction

data object FcmpgInstruction: ArithmeticInstruction

data object DcmplInstruction: ArithmeticInstruction

data object DcmpgInstruction: ArithmeticInstruction

data class GotoInstruction(val target: Label) : Instruction

data object JsrInstruction: Instruction

data object RetInstruction: Instruction

sealed interface SwitchInstruction: Instruction {
    val default: Label
    val cases: Map<Int, Label>
}

data class TableSwitchInstruction(override val default: Label, override val cases: Map<Int, Label>) : SwitchInstruction

data class LookupSwitchInstruction(override val default: Label, override val cases: Map<Int, Label>) : SwitchInstruction

fun SwitchInstruction(default: Label, cases: Map<Int, Label>): SwitchInstruction {
    val keys = cases.keys
    if (keys.isEmpty()) {
        return LookupSwitchInstruction(default, cases)
    }

    val lo = keys.min()
    val hi = keys.max()
    val nLabels = keys.size

    // javac's cost heuristic: compare tableswitch vs lookupswitch cost
    val tableSpaceCost = 4L + (hi.toLong() - lo.toLong() + 1L)
    val tableTimeCost = 3L
    val lookupSpaceCost = 3L + 2L * nLabels
    val lookupTimeCost = nLabels.toLong()

    val useTableSwitch =
        tableSpaceCost + 3 * tableTimeCost <= lookupSpaceCost + 3 * lookupTimeCost

    if (useTableSwitch) {
        return TableSwitchInstruction(default, cases)
    } else {
        return LookupSwitchInstruction(default, cases)
    }
}

sealed interface ReturnInstruction : Instruction

data object IReturnInstruction: ReturnInstruction

data object LReturnInstruction: ReturnInstruction

data object FReturnInstruction: ReturnInstruction

data object DReturnInstruction: ReturnInstruction

data object AReturnInstruction: ReturnInstruction

data object VReturnInstruction: ReturnInstruction

fun ReturnInstruction(type: Type) : ReturnInstruction = when (type) {
    VoidType -> VReturnInstruction
    IntType, BooleanType, ByteType, CharType, ShortType -> IReturnInstruction
    LongType -> LReturnInstruction
    FloatType -> FReturnInstruction
    DoubleType -> DReturnInstruction
    else -> AReturnInstruction
}

// ---

sealed class AbstractGetFieldInstruction(val field: FieldSignature) : Instruction

sealed class AbstractPutFieldInstruction(val field: FieldSignature) : Instruction

class GetStaticInstruction(field: FieldSignature) : AbstractGetFieldInstruction(field)

class PutStaticInstruction(field: FieldSignature) : AbstractPutFieldInstruction(field)

class GetFieldInstruction(field: FieldSignature) : AbstractGetFieldInstruction(field)

class PutFieldInstruction(field: FieldSignature) : AbstractPutFieldInstruction(field)


// ── method invocation ────────────────────────────────────────────────────────

sealed interface InvocationInstruction : Instruction

sealed interface KnownTargetInvocationInstruction : InvocationInstruction {
    val owner: ClassType
    val method: MethodSignature
}

sealed interface DirectInvocationInstruction : KnownTargetInvocationInstruction

sealed interface IndirectInvocationInstruction : KnownTargetInvocationInstruction

class InvokeVirtualInstruction(
    override val owner: ClassType,
    override val method: MethodSignature,
): IndirectInvocationInstruction

class InvokeSpecialInstruction(
    override val owner: ClassType,
    override val method: MethodSignature,
): DirectInvocationInstruction

class InvokeStaticInstruction(
    override val owner: ClassType,
    override val method: MethodSignature,
): DirectInvocationInstruction

class InvokeInterfaceInstruction(
    override val owner: ClassType,
    override val method: MethodSignature,
): IndirectInvocationInstruction

class InvokeDynamicInstruction(
    val callSiteSignature: MethodSignature,
    val bootstrapMethod: MethodHandle,
    val bootstrapArguments: List<BootstrapArgument>
): InvocationInstruction

enum class HandleKind {
    GET_FIELD,
    GET_STATIC,
    PUT_FIELD,
    PUT_STATIC,
    INVOKE_VIRTUAL,
    INVOKE_STATIC,
    INVOKE_SPECIAL,
    NEW_INVOKE_SPECIAL,
    INVOKE_INTERFACE,
}

data class MethodHandle(
    val kind: HandleKind,
    val owner: ClassType,
    val name: String,
    val descriptor: String,
    val isInterface: Boolean,
) {
    val isFieldHandle get() = kind in setOf(HandleKind.GET_FIELD, HandleKind.GET_STATIC, HandleKind.PUT_FIELD, HandleKind.PUT_STATIC)
    val isMethodHandle get() = !isFieldHandle
}

sealed interface BootstrapArgument {
    data class IntArg(val value: Int) : BootstrapArgument
    data class FloatArg(val value: Float) : BootstrapArgument
    data class LongArg(val value: Long) : BootstrapArgument
    data class DoubleArg(val value: Double) : BootstrapArgument
    data class StringArg(val value: String) : BootstrapArgument
    data class TypeArg(val value: ClassType) : BootstrapArgument
    data class HandleArg(val value: MethodHandle): BootstrapArgument
    data class DynamicArg(val value: ConstantDynamic) : BootstrapArgument
}

data class ConstantDynamic(
    val name: String,
    val type: Type,
    val bootstrapMethod: MethodHandle,
    val bootstrapArguments: List<BootstrapArgument>
)

data class NewInstruction(val type: ClassType) : Instruction

data class NewArrayInstruction(val elementType: Type, val dimensions: Int) : Instruction

data class ANewArrayInstruction(val elementType: Type, val dimensions: Int) : Instruction

data object ArrayLengthInstruction: Instruction

data object AThrowInstruction : Instruction

data class CheckCastInstruction(val type: ClassType) : Instruction

data class InstanceOfInstruction(val type: ClassType) : Instruction

sealed interface MonitorInstruction: Instruction

data object MonitorEnterInstruction : MonitorInstruction

data object MonitorExitInstruction : MonitorInstruction

data class MultiANewArrayInstruction(val elementType: Type, val dimensions: Int) : Instruction

// ── control flow ─────────────────────────────────────────────────────────────


sealed interface JumpInstruction : Instruction {
    val target: Label
}

data class LabelInstruction(val label: Label) : Instruction

data class IfEqInstruction(override val target: Label) : JumpInstruction
data class IfNeInstruction(override val target: Label) : JumpInstruction
data class IfLtInstruction(override val target: Label) : JumpInstruction
data class IfGeInstruction(override val target: Label) : JumpInstruction
data class IfGtInstruction(override val target: Label) : JumpInstruction
data class IfLeInstruction(override val target: Label) : JumpInstruction

data class IfICmpEqInstruction(override val target: Label) : JumpInstruction
data class IfICmpNeInstruction(override val target: Label) : JumpInstruction
data class IfICmpLtInstruction(override val target: Label) : JumpInstruction
data class IfICmpGeInstruction(override val target: Label) : JumpInstruction
data class IfICmpGtInstruction(override val target: Label) : JumpInstruction
data class IfICmpLeInstruction(override val target: Label) : JumpInstruction

data class IfACmpEqInstruction(override val target: Label) : JumpInstruction
data class IfACmpNeInstruction(override val target: Label) : JumpInstruction

data class IfNullInstruction(override val target: Label) : JumpInstruction
data class IfNonNullInstruction(override val target: Label) : JumpInstruction

data class LineNumberInstruction(val line: Int) : Instruction