package com.leko.kvm.bytecode

import com.leko.kvm.FieldSignature
import com.leko.kvm.MethodDescriptor
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

/**
 * Uniquely identifies a bytecode location within a method body.
 *
 * Labels are used by branch instructions, exception handlers, and code placement
 * operations to reference target positions in the instruction stream. A label is
 * placed in the stream by a [LabelInstruction] and referenced by jumps, switches
 * and exception handlers.
 *
 * Equality is by [name], so two labels with the same name are the same location.
 * The default name is a random UUID, which makes `Label()` unique in practice.
 *
 * @property name The label's identity. Randomly generated unless given.
 */
data class Label @OptIn(ExperimentalUuidApi::class) constructor(val name: String = Uuid.random().toString())

/**
 * A single JVM instruction emitted into a method body.
 *
 * Instruction objects model the bytecode-level operations of the KVM DSL without
 * depending on ASM-specific types. The hierarchy is sealed.
 *
 * Instructions with operands whose type is a plain `class` (loads, stores, field
 * access and invocations) have identity equality. Data classes and data objects
 * have structural equality.
 */
sealed interface Instruction

/**
 * Instruction that can be used as a constant-loading or immediate-value opcode.
 *
 * @see IntConstant
 * @see LongConstant
 * @see FloatConstant
 * @see DoubleConstant
 */
sealed interface ConstantInstruction : Instruction

/** `nop`: does nothing. */
data object NopInstruction : Instruction

// ── constants ────────────────────────────────────────────────────────────────

/** `aconst_null`: pushes `null`. */
data object AConstNullInstruction : ConstantInstruction

/** `iconst_m1`: pushes the int `-1`. */
data object IConstM1Instruction : ConstantInstruction

/** `iconst_0`: pushes the int `0`. */
data object IConst0Instruction : ConstantInstruction

/** `iconst_1`: pushes the int `1`. */
data object IConst1Instruction : ConstantInstruction

/** `iconst_2`: pushes the int `2`. */
data object IConst2Instruction : ConstantInstruction

/** `iconst_3`: pushes the int `3`. */
data object IConst3Instruction : ConstantInstruction

/** `iconst_4`: pushes the int `4`. */
data object IConst4Instruction : ConstantInstruction

/** `iconst_5`: pushes the int `5`. */
data object IConst5Instruction : ConstantInstruction

/**
 * Returns the most compact instruction that pushes the int [value]:
 * `iconst_*` for -1..5, `bipush` for the byte range, `sipush` for the short
 * range, and `ldc` otherwise.
 */
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

/** `lconst_0`: pushes the long `0L`. */
data object LConst0Instruction : ConstantInstruction

/** `lconst_1`: pushes the long `1L`. */
data object LConst1Instruction : ConstantInstruction

/**
 * Returns the most compact instruction that pushes the long [value]:
 * `lconst_0`, `lconst_1`, or `ldc2_w`.
 */
fun LongConstant(value: Long) : ConstantInstruction = when (value) {
    0L -> LConst0Instruction
    1L -> LConst1Instruction
    else -> Ldc2wInstruction(value)
}

/** `fconst_0`: pushes the float `0.0f`. */
data object FConst0Instruction : ConstantInstruction

/** `fconst_1`: pushes the float `1.0f`. */
data object FConst1Instruction : ConstantInstruction

/** `fconst_2`: pushes the float `2.0f`. */
data object FConst2Instruction : ConstantInstruction

/**
 * Returns the most compact instruction that pushes the float [value]:
 * `fconst_0`, `fconst_1`, `fconst_2`, or `ldc`.
 */
fun FloatConstant(value: Float) : ConstantInstruction = when (value) {
    0f if value.toRawBits() == 0 -> FConst0Instruction
    1f -> FConst1Instruction
    2f -> FConst2Instruction
    else -> LdcInstruction(value)
}

/** `dconst_0`: pushes the double `0.0`. */
data object DConst0Instruction : ConstantInstruction

/** `dconst_1`: pushes the double `1.0`. */
data object DConst1Instruction : ConstantInstruction

/**
 * Returns the most compact instruction that pushes the double [value]:
 * `dconst_0`, `dconst_1`, or `ldc2_w`.
 */
fun DoubleConstant(value: Double) : ConstantInstruction = when (value) {
    0.0 if value.toRawBits() == 0L -> DConst0Instruction
    1.0 -> DConst1Instruction
    else -> Ldc2wInstruction(value)
}

/**
 * `bipush`: pushes a byte, sign-extended to an int.
 *
 * @property value The byte to push.
 */
data class BipushInstruction(val value: Byte) : ConstantInstruction

/**
 * `sipush`: pushes a short, sign-extended to an int.
 *
 * @property value The short to push.
 */
data class SipushInstruction(val value: Short) : ConstantInstruction

/**
 * `ldc`: pushes a single-slot constant from the constant pool.
 *
 * The value is untyped. Valid values are `Int`, `Float`, `String`, and
 * class, method type or method handle constants, as accepted by the code
 * that writes the class file.
 *
 * @property value The constant to push.
 */
data class LdcInstruction(val value: Any) : ConstantInstruction

/**
 * `ldc2_w`: pushes a two-slot constant, a `long` or a `double`.
 *
 * @property value The constant. Must be a [Long] or a [Double].
 * @throws IllegalArgumentException if [value] is any other kind of number.
 */
data class Ldc2wInstruction(val value: Number) : ConstantInstruction {
    init {
        require(value is Double || value is Long) { "value must be either Long or Double" }
    }
}

// ── loads and stores ─────────────────────────────────────────────────────────

/**
 * Loads a local variable from the given JVM register.
 *
 * Subclasses represent the concrete bytecode opcodes used for primitive and
 * reference locals.
 *
 * @property register The local variable slot index.
 */
sealed class VarLoadInstruction(val register: Int) : Instruction

/**
 * Loads a value from an array slot.
 */
sealed interface ArrayLoadInstruction : Instruction

/** `iload`: loads an int (also boolean, byte, char or short) from a local. */
class ILoadInstruction(register: Int): VarLoadInstruction(register)

/** `lload`: loads a long from a local. */
class LLoadInstruction(register: Int): VarLoadInstruction(register)

/** `fload`: loads a float from a local. */
class FLoadInstruction(register: Int): VarLoadInstruction(register)

/** `dload`: loads a double from a local. */
class DLoadInstruction(register: Int): VarLoadInstruction(register)

/** `aload`: loads a reference from a local. */
class ALoadInstruction(register: Int): VarLoadInstruction(register)

/**
 * Resolves the JVM local-load opcode for the supplied type.
 *
 * @param type type of the local variable.
 * @param register local variable slot index.
 * @return corresponding load instruction for the JVM operand stack.
 * @throws IllegalStateException if [type] is [VoidType].
 */
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

/** `iaload`: loads an int from an `int[]`. */
data object IALoadInstruction: ArrayLoadInstruction

/** `laload`: loads a long from a `long[]`. */
data object LALoadInstruction: ArrayLoadInstruction

/** `faload`: loads a float from a `float[]`. */
data object FALoadInstruction: ArrayLoadInstruction

/** `daload`: loads a double from a `double[]`. */
data object DALoadInstruction: ArrayLoadInstruction

/** `aaload`: loads a reference from an array of references. */
data object AALoadInstruction: ArrayLoadInstruction

/** `baload`: loads a byte or boolean from a `byte[]` or `boolean[]`. */
data object BALoadInstruction: ArrayLoadInstruction

/** `caload`: loads a char from a `char[]`. */
data object CALoadInstruction: ArrayLoadInstruction

/** `saload`: loads a short from a `short[]`. */
data object SALoadInstruction: ArrayLoadInstruction

/**
 * Resolves the JVM array-load opcode for the supplied element type.
 *
 * boolean and byte both map to [BALoadInstruction], since the JVM uses
 * baload for both byte[] and boolean[].
 *
 * @throws IllegalStateException if [type] is [VoidType].
 */
fun ArrayLoadInstruction(type: Type): ArrayLoadInstruction = when(type) {
    BooleanType -> BALoadInstruction
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

/**
 * Stores a value into a local variable slot.
 *
 * @property register The local variable slot index.
 */
sealed class VarStoreInstruction(val register: Int) : Instruction

/**
 * Stores a value into an array element.
 */
sealed interface ArrayStoreInstruction : Instruction

/** `istore`: stores an int (also boolean, byte, char or short) into a local. */
class IStoreInstruction(register: Int): VarStoreInstruction(register)

/** `lstore`: stores a long into a local. */
class LStoreInstruction(register: Int): VarStoreInstruction(register)

/** `fstore`: stores a float into a local. */
class FStoreInstruction(register: Int): VarStoreInstruction(register)

/** `dstore`: stores a double into a local. */
class DStoreInstruction(register: Int): VarStoreInstruction(register)

/** `astore`: stores a reference into a local. */
class AStoreInstruction(register: Int): VarStoreInstruction(register)

/**
 * Resolves the JVM local-store opcode for the supplied type.
 *
 * @throws IllegalStateException if [type] is [VoidType].
 */
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

/** `iastore`: stores an int into an `int[]`. */
data object IAStoreInstruction: ArrayStoreInstruction

/** `lastore`: stores a long into a `long[]`. */
data object LAStoreInstruction: ArrayStoreInstruction

/** `fastore`: stores a float into a `float[]`. */
data object FAStoreInstruction: ArrayStoreInstruction

/** `dastore`: stores a double into a `double[]`. */
data object DAStoreInstruction: ArrayStoreInstruction

/** `aastore`: stores a reference into an array of references. */
data object AAStoreInstruction: ArrayStoreInstruction

/** `bastore`: stores a byte or boolean into a `byte[]` or `boolean[]`. */
data object BAStoreInstruction: ArrayStoreInstruction

/** `castore`: stores a char into a `char[]`. */
data object CAStoreInstruction: ArrayStoreInstruction

/** `sastore`: stores a short into a `short[]`. */
data object SAStoreInstruction: ArrayStoreInstruction

/**
 * Resolves the JVM array-store opcode for the supplied element type.
 *
 * boolean and byte both map to `BAStoreInstruction`, since the JVM uses
 * baload for both byte[] and boolean[].
 *
 * @throws IllegalStateException if [type] is [VoidType].
 */
fun ArrayStoreInstruction(type: Type): ArrayStoreInstruction = when(type) {
    BooleanType -> BAStoreInstruction
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

// ── stack manipulation ───────────────────────────────────────────────────────

/**
 * Operations that manipulate or duplicate values on the JVM operand stack.
 */
sealed interface StackOperationInstruction : Instruction

/** `pop`: discards the top single-slot value. */
data object PopInstruction: StackOperationInstruction

/** `pop2`: discards the top two slots (one long or double, or two single-slot values). */
data object Pop2Instruction: StackOperationInstruction

/** `dup`: duplicates the top single-slot value. */
data object DupInstruction: StackOperationInstruction

/** `dup_x1`: duplicates the top value and inserts the copy beneath the second value. */
data object DupX1Instruction: StackOperationInstruction

/** `dup_x2`: duplicates the top value and inserts the copy beneath the next two slots. */
data object DupX2Instruction: StackOperationInstruction

/** `dup2`: duplicates the top two slots. */
data object Dup2Instruction: StackOperationInstruction

/** `dup2_x1`: duplicates the top two slots and inserts the copy beneath the third slot. */
data object Dup2X1Instruction: StackOperationInstruction

/** `dup2_x2`: duplicates the top two slots and inserts the copy beneath the next two slots. */
data object Dup2X2Instruction: StackOperationInstruction

/** `swap`: swaps the top two single-slot values. */
data object SwapInstruction: StackOperationInstruction

// ── arithmetic, conversion and comparison ────────────────────────────────────

/**
 * JVM arithmetic, conversion, or comparison instruction.
 *
 * The leading letter of each name is the operand type: `I` int, `L` long,
 * `F` float, `D` double.
 */
sealed interface ArithmeticInstruction: Instruction

/** `iadd`: int addition. */
data object IAddInstruction: ArithmeticInstruction

/** `ladd`: long addition. */
data object LAddInstruction: ArithmeticInstruction

/** `fadd`: float addition. */
data object FAddInstruction: ArithmeticInstruction

/** `dadd`: double addition. */
data object DAddInstruction: ArithmeticInstruction

/** `isub`: int subtraction. */
data object ISubInstruction: ArithmeticInstruction

/** `lsub`: long subtraction. */
data object LSubInstruction: ArithmeticInstruction

/** `fsub`: float subtraction. */
data object FSubInstruction: ArithmeticInstruction

/** `dsub`: double subtraction. */
data object DSubInstruction: ArithmeticInstruction

/** `imul`: int multiplication. */
data object IMulInstruction: ArithmeticInstruction

/** `lmul`: long multiplication. */
data object LMulInstruction: ArithmeticInstruction

/** `fmul`: float multiplication. */
data object FMulInstruction: ArithmeticInstruction

/** `dmul`: double multiplication. */
data object DMulInstruction: ArithmeticInstruction

/** `idiv`: int division. Throws `ArithmeticException` on division by zero. */
data object IDivInstruction: ArithmeticInstruction

/** `ldiv`: long division. Throws `ArithmeticException` on division by zero. */
data object LDivInstruction: ArithmeticInstruction

/** `fdiv`: float division. */
data object FDivInstruction: ArithmeticInstruction

/** `ddiv`: double division. */
data object DDivInstruction: ArithmeticInstruction

/** `irem`: int remainder. Throws `ArithmeticException` on division by zero. */
data object IRemInstruction: ArithmeticInstruction

/** `lrem`: long remainder. Throws `ArithmeticException` on division by zero. */
data object LRemInstruction: ArithmeticInstruction

/** `frem`: float remainder. */
data object FRemInstruction: ArithmeticInstruction

/** `drem`: double remainder. */
data object DRemInstruction: ArithmeticInstruction

/** `ineg`: int negation. */
data object INegInstruction: ArithmeticInstruction

/** `lneg`: long negation. */
data object LNegInstruction: ArithmeticInstruction

/** `fneg`: float negation. */
data object FNegInstruction: ArithmeticInstruction

/** `dneg`: double negation. */
data object DNegInstruction: ArithmeticInstruction

/** `ishl`: int shift left. */
data object IShlInstruction: ArithmeticInstruction

/** `lshl`: long shift left. */
data object LShlInstruction: ArithmeticInstruction

/** `ishr`: int arithmetic (sign-preserving) shift right. */
data object IShrInstruction: ArithmeticInstruction

/** `lshr`: long arithmetic (sign-preserving) shift right. */
data object LShrInstruction: ArithmeticInstruction

/** `iushr`: int logical (unsigned) shift right. */
data object IUshrInstruction: ArithmeticInstruction

/** `lushr`: long logical (unsigned) shift right. */
data object LUshrInstruction: ArithmeticInstruction

/** `iand`: int bitwise AND. */
data object IAndInstruction: ArithmeticInstruction

/** `land`: long bitwise AND. */
data object LAndInstruction: ArithmeticInstruction

/** `ior`: int bitwise OR. */
data object IOrInstruction: ArithmeticInstruction

/** `lor`: long bitwise OR. */
data object LOrInstruction: ArithmeticInstruction

/** `ixor`: int bitwise XOR. */
data object IXorInstruction: ArithmeticInstruction

/** `lxor`: long bitwise XOR. */
data object LXorInstruction: ArithmeticInstruction

/**
 * `iinc`: adds a constant to an int local variable in place.
 *
 * @property index The local variable slot index.
 * @property amount The signed constant to add. Defaults to 1.
 */
data class IIncInstruction(val index: Int, val amount: Int = 1) : Instruction

/** `i2l`: int to long. */
data object I2LInstruction: ArithmeticInstruction

/** `i2f`: int to float. */
data object I2FInstruction: ArithmeticInstruction

/** `i2d`: int to double. */
data object I2DInstruction: ArithmeticInstruction

/** `l2i`: long to int. */
data object L2IInstruction: ArithmeticInstruction

/** `l2f`: long to float. */
data object L2FInstruction: ArithmeticInstruction

/** `l2d`: long to double. */
data object L2DInstruction: ArithmeticInstruction

/** `f2i`: float to int. */
data object F2IInstruction: ArithmeticInstruction

/** `f2l`: float to long. */
data object F2LInstruction: ArithmeticInstruction

/** `f2d`: float to double. */
data object F2DInstruction: ArithmeticInstruction

/** `d2i`: double to int. */
data object D2IInstruction: ArithmeticInstruction

/** `d2l`: double to long. */
data object D2LInstruction: ArithmeticInstruction

/** `d2f`: double to float. */
data object D2FInstruction: ArithmeticInstruction

/** `i2b`: int truncated to byte. */
data object I2BInstruction: ArithmeticInstruction

/** `i2c`: int truncated to char. */
data object I2CInstruction: ArithmeticInstruction

/** `i2s`: int truncated to short. */
data object I2SInstruction: ArithmeticInstruction

/** `lcmp`: compares two longs and pushes -1, 0 or 1. */
data object LcmpInstruction: ArithmeticInstruction

/** `fcmpl`: compares two floats and pushes -1, 0 or 1. NaN yields -1. */
data object FcmplInstruction: ArithmeticInstruction

/** `fcmpg`: compares two floats and pushes -1, 0 or 1. NaN yields 1. */
data object FcmpgInstruction: ArithmeticInstruction

/** `dcmpl`: compares two doubles and pushes -1, 0 or 1. NaN yields -1. */
data object DcmplInstruction: ArithmeticInstruction

/** `dcmpg`: compares two doubles and pushes -1, 0 or 1. NaN yields 1. */
data object DcmpgInstruction: ArithmeticInstruction

// ── control flow ─────────────────────────────────────────────────────────────

/**
 * Unconditional jump to another label.
 *
 * @property target destination instruction label.
 */
data class GotoInstruction(val target: Label) : Instruction

/**
 * `jsr`: jump to subroutine. Legacy opcode, not allowed in class files of
 * version 51 and above. It carries no target, so it cannot be fully represented.
 */
data object JsrInstruction: Instruction

/**
 * `ret`: return from subroutine. Legacy counterpart of [JsrInstruction],
 * carrying no operand.
 */
data object RetInstruction: Instruction

/**
 * Bytecode switch dispatch based on an integer key.
 *
 * @property default The label jumped to when no case matches.
 * @property cases The case keys and the label each one jumps to.
 */
sealed interface SwitchInstruction: Instruction {
    val default: Label
    val cases: Map<Int, Label>
}

/**
 * `tableswitch`: switch over a dense range of keys, using a jump table.
 * Compact and constant-time when the keys are close together.
 */
data class TableSwitchInstruction(override val default: Label, override val cases: Map<Int, Label>) : SwitchInstruction

/**
 * `lookupswitch`: switch over sparse keys, using a sorted key/label table.
 * Used when a jump table would waste space.
 */
data class LookupSwitchInstruction(override val default: Label, override val cases: Map<Int, Label>) : SwitchInstruction

/**
 * Creates the appropriate switch instruction variant for the supplied case map.
 *
 * Chooses between [TableSwitchInstruction] and [LookupSwitchInstruction] with
 * javac's space/time cost heuristic. An empty [cases] map gives a lookup switch.
 */
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

/**
 * Instruction that returns a value from the current method.
 */
sealed interface ReturnInstruction : Instruction

/** `ireturn`: returns an int (also boolean, byte, char or short). */
data object IReturnInstruction: ReturnInstruction

/** `lreturn`: returns a long. */
data object LReturnInstruction: ReturnInstruction

/** `freturn`: returns a float. */
data object FReturnInstruction: ReturnInstruction

/** `dreturn`: returns a double. */
data object DReturnInstruction: ReturnInstruction

/** `areturn`: returns a reference. */
data object AReturnInstruction: ReturnInstruction

/** `return`: returns from a `void` method. */
data object VReturnInstruction: ReturnInstruction

/**
 * Resolves the JVM return opcode for the given method return type.
 *
 * Any type that is not void or a primitive gives [AReturnInstruction].
 */
fun ReturnInstruction(type: Type) : ReturnInstruction = when (type) {
    VoidType -> VReturnInstruction
    IntType, BooleanType, ByteType, CharType, ShortType -> IReturnInstruction
    LongType -> LReturnInstruction
    FloatType -> FReturnInstruction
    DoubleType -> DReturnInstruction
    else -> AReturnInstruction
}

// ── fields ───────────────────────────────────────────────────────────────────

/**
 * Base of the field read instructions ([GetStaticInstruction], [GetFieldInstruction]).
 *
 * @property field The accessed field's owner, name and type.
 */
sealed class AbstractGetFieldInstruction(val field: FieldSignature) : Instruction

/**
 * Base of the field write instructions ([PutStaticInstruction], [PutFieldInstruction]).
 *
 * @property field The accessed field's owner, name and type.
 */
sealed class AbstractPutFieldInstruction(val field: FieldSignature) : Instruction

/** `getstatic`: pushes the value of a static field. */
class GetStaticInstruction(field: FieldSignature) : AbstractGetFieldInstruction(field)

/** `putstatic`: pops a value into a static field. */
class PutStaticInstruction(field: FieldSignature) : AbstractPutFieldInstruction(field)

/** `getfield`: pops an object reference and pushes the value of its instance field. */
class GetFieldInstruction(field: FieldSignature) : AbstractGetFieldInstruction(field)

/** `putfield`: pops a value and an object reference, and stores the value in the instance field. */
class PutFieldInstruction(field: FieldSignature) : AbstractPutFieldInstruction(field)


// ── method invocation ────────────────────────────────────────────────────────

/**
 * Any method invocation instruction, including `invokedynamic`.
 *
 * @see KnownTargetInvocationInstruction
 */
sealed interface InvocationInstruction : Instruction

/**
 * An invocation whose static target is a named method on a named owner.
 * Everything except `invokedynamic`.
 *
 * @property owner The class or interface the method is looked up on.
 * @property method The method's name and descriptor.
 */
sealed interface KnownTargetInvocationInstruction : InvocationInstruction {
    val owner: ClassType
    val method: MethodSignature
}

/**
 * An invocation that always runs one statically known method:
 * [InvokeSpecialInstruction] and [InvokeStaticInstruction].
 */
sealed interface DirectInvocationInstruction : KnownTargetInvocationInstruction

/**
 * An invocation dispatched on the receiver's runtime type, so any override may run:
 * [InvokeVirtualInstruction] and [InvokeInterfaceInstruction].
 */
sealed interface IndirectInvocationInstruction : KnownTargetInvocationInstruction

/** `invokevirtual`: calls an instance method, with dynamic dispatch on the receiver. */
class InvokeVirtualInstruction(
    override val owner: ClassType,
    override val method: MethodSignature,
): IndirectInvocationInstruction

/** `invokespecial`: calls a constructor, a private method or a superclass method without dynamic dispatch. */
class InvokeSpecialInstruction(
    override val owner: ClassType,
    override val method: MethodSignature,
): DirectInvocationInstruction

/** `invokestatic`: calls a static method. */
class InvokeStaticInstruction(
    override val owner: ClassType,
    override val method: MethodSignature,
): DirectInvocationInstruction

/** `invokeinterface`: calls an interface method, with dynamic dispatch on the receiver. */
class InvokeInterfaceInstruction(
    override val owner: ClassType,
    override val method: MethodSignature,
): IndirectInvocationInstruction

/**
 * `invokedynamic`: a call site linked at runtime by a bootstrap method.
 * Used for lambdas, method references and string concatenation, among others.
 *
 * It has no static owner or target, so it is not a [KnownTargetInvocationInstruction].
 *
 * @property callSiteSignature The name and descriptor of the call site.
 * @property bootstrapMethod The method that links the call site.
 * @property bootstrapArguments The static arguments passed to the bootstrap method.
 */
class InvokeDynamicInstruction(
    val callSiteSignature: MethodSignature,
    val bootstrapMethod: MethodHandle,
    val bootstrapArguments: List<BootstrapArgument>
): InvocationInstruction

/**
 * The kind of a [MethodHandle], matching the `REF_*` constants of the class file
 * format (JVMS §5.4.3.5).
 */
enum class HandleKind {
    /** `REF_getField` */
    GET_FIELD,
    /** `REF_getStatic` */
    GET_STATIC,
    /** `REF_putField` */
    PUT_FIELD,
    /** `REF_putStatic` */
    PUT_STATIC,
    /** `REF_invokeVirtual` */
    INVOKE_VIRTUAL,
    /** `REF_invokeStatic` */
    INVOKE_STATIC,
    /** `REF_invokeSpecial` */
    INVOKE_SPECIAL,
    /** `REF_newInvokeSpecial`: a constructor reference. */
    NEW_INVOKE_SPECIAL,
    /** `REF_invokeInterface` */
    INVOKE_INTERFACE,
}

/**
 * A constant method handle, as used for bootstrap methods and bootstrap arguments.
 * This models the class file constant, not `java.lang.invoke.MethodHandle`.
 *
 * @property kind What the handle does: field access or a kind of invocation.
 * @property owner The class holding the referenced field or method.
 * @property name The referenced field or method name.
 * @property descriptor The raw JVM descriptor string of the field or method.
 * @property isInterface Whether [owner] is an interface.
 */
data class MethodHandle(
    val kind: HandleKind,
    val owner: ClassType,
    val name: String,
    val descriptor: String,
    val isInterface: Boolean,
) {
    /** `true` if this handle refers to a field (one of the four get/put kinds). */
    val isFieldHandle get() = kind in setOf(HandleKind.GET_FIELD, HandleKind.GET_STATIC, HandleKind.PUT_FIELD, HandleKind.PUT_STATIC)

    /** `true` if this handle refers to a method or constructor. */
    val isMethodHandle get() = !isFieldHandle
}

/**
 * A static argument of a bootstrap method: one of the constant kinds that can
 * appear in the `BootstrapMethods` attribute.
 */
sealed interface BootstrapArgument {
    /** An `int` constant. */
    data class IntArg(val value: Int) : BootstrapArgument
    /** A `float` constant. */
    data class FloatArg(val value: Float) : BootstrapArgument
    /** A `long` constant. */
    data class LongArg(val value: Long) : BootstrapArgument
    /** A `double` constant. */
    data class DoubleArg(val value: Double) : BootstrapArgument
    /** A `String` constant. */
    data class StringArg(val value: String) : BootstrapArgument
    /** A class constant. */
    data class TypeArg(val value: ClassType) : BootstrapArgument
    /** A method type constant. */
    data class MethodTypeArg(val value: MethodDescriptor): BootstrapArgument
    /** A method handle constant. */
    data class HandleArg(val value: MethodHandle): BootstrapArgument
    /** A dynamically computed constant. */
    data class DynamicArg(val value: ConstantDynamic) : BootstrapArgument
}

/**
 * A dynamically computed constant (`CONSTANT_Dynamic`), resolved by a bootstrap
 * method the first time it is used.
 *
 * @property name The constant's name, passed to the bootstrap method.
 * @property type The constant's type.
 * @property bootstrapMethod The method that computes the constant.
 * @property bootstrapArguments The static arguments passed to the bootstrap method.
 */
data class ConstantDynamic(
    val name: String,
    val type: Type,
    val bootstrapMethod: MethodHandle,
    val bootstrapArguments: List<BootstrapArgument>
)

// ── objects and arrays ───────────────────────────────────────────────────────

/**
 * `new`: allocates an uninitialized instance of [type]. A constructor call
 * (`invokespecial <init>`) must follow before the object is used.
 *
 * @property type The class to instantiate.
 */
data class NewInstruction(val type: ClassType) : Instruction

/**
 * `newarray`: creates an array of a primitive element type.
 *
 * @property elementType The primitive element type.
 * @property dimensions The array's dimensions.
 */
data class NewArrayInstruction(val elementType: Type, val dimensions: Int) : Instruction

/**
 * `anewarray`: creates an array of references.
 *
 * @property elementType The element type.
 * @property dimensions The array's dimensions.
 */
data class ANewArrayInstruction(val elementType: Type, val dimensions: Int) : Instruction

/** `arraylength`: pushes the length of an array. */
data object ArrayLengthInstruction: Instruction

/** `athrow`: throws the exception on top of the stack. Ends the block in a [com.leko.kvm.cfg.ThrowBlock]. */
data object AThrowInstruction : Instruction

/**
 * `checkcast`: throws `ClassCastException` unless the top reference is `null`
 * or an instance of [type].
 */
data class CheckCastInstruction(val type: ReferenceType) : Instruction

/** `instanceof`: pushes 1 if the top reference is an instance of [type], otherwise 0. */
data class InstanceOfInstruction(val type: ReferenceType) : Instruction

/** Monitor instructions, used to implement `synchronized` blocks. */
sealed interface MonitorInstruction: Instruction

/** `monitorenter`: acquires the monitor of the object on top of the stack. */
data object MonitorEnterInstruction : MonitorInstruction

/** `monitorexit`: releases the monitor of the object on top of the stack. */
data object MonitorExitInstruction : MonitorInstruction

/**
 * `multianewarray`: creates a multidimensional array.
 *
 * @property elementType The element type.
 * @property dimensions The number of dimensions to allocate.
 */
data class MultiANewArrayInstruction(val elementType: Type, val dimensions: Int) : Instruction

// ── branches and pseudo-instructions ─────────────────────────────────────────

/**
 * A conditional jump. Control continues at [target] if the condition holds
 * and falls through to the next instruction otherwise.
 *
 * Unconditional jumps are [GotoInstruction], not part of this hierarchy.
 *
 * @property target The label jumped to when the condition holds.
 */
sealed interface JumpInstruction : Instruction {
    val target: Label
}

/**
 * Pseudo-instruction that marks the position of [label] in the instruction
 * stream. It emits no bytecode.
 *
 * @property label The label placed at this position.
 */
data class LabelInstruction(val label: Label) : Instruction

/** `ifeq`: jumps if the int on top of the stack is 0. */
data class IfEqInstruction(override val target: Label) : JumpInstruction
/** `ifne`: jumps if the int on top of the stack is not 0. */
data class IfNeInstruction(override val target: Label) : JumpInstruction
/** `iflt`: jumps if the int on top of the stack is less than 0. */
data class IfLtInstruction(override val target: Label) : JumpInstruction
/** `ifge`: jumps if the int on top of the stack is greater than or equal to 0. */
data class IfGeInstruction(override val target: Label) : JumpInstruction
/** `ifgt`: jumps if the int on top of the stack is greater than 0. */
data class IfGtInstruction(override val target: Label) : JumpInstruction
/** `ifle`: jumps if the int on top of the stack is less than or equal to 0. */
data class IfLeInstruction(override val target: Label) : JumpInstruction

/** `if_icmpeq`: jumps if the two ints on top of the stack are equal. */
data class IfICmpEqInstruction(override val target: Label) : JumpInstruction
/** `if_icmpne`: jumps if the two ints on top of the stack are not equal. */
data class IfICmpNeInstruction(override val target: Label) : JumpInstruction
/** `if_icmplt`: jumps if the second int is less than the top int. */
data class IfICmpLtInstruction(override val target: Label) : JumpInstruction
/** `if_icmpge`: jumps if the second int is greater than or equal to the top int. */
data class IfICmpGeInstruction(override val target: Label) : JumpInstruction
/** `if_icmpgt`: jumps if the second int is greater than the top int. */
data class IfICmpGtInstruction(override val target: Label) : JumpInstruction
/** `if_icmple`: jumps if the second int is less than or equal to the top int. */
data class IfICmpLeInstruction(override val target: Label) : JumpInstruction

/** `if_acmpeq`: jumps if the two references on top of the stack are identical. */
data class IfACmpEqInstruction(override val target: Label) : JumpInstruction
/** `if_acmpne`: jumps if the two references on top of the stack are not identical. */
data class IfACmpNeInstruction(override val target: Label) : JumpInstruction

/** `ifnull`: jumps if the reference on top of the stack is `null`. */
data class IfNullInstruction(override val target: Label) : JumpInstruction
/** `ifnonnull`: jumps if the reference on top of the stack is not `null`. */
data class IfNonNullInstruction(override val target: Label) : JumpInstruction

/**
 * Pseudo-instruction recording the source line of the instructions that follow.
 * It emits no bytecode of its own (it becomes an entry of the `LineNumberTable`).
 *
 * @property line The source line number.
 */
data class LineNumberInstruction(val line: Int) : Instruction