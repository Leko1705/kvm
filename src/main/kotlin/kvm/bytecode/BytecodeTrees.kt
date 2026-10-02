package com.leko.kvm.bytecode

import com.leko.kvm.FieldSignature
import com.leko.kvm.MethodDescriptor
import com.leko.kvm.MethodSignature
import com.leko.kvm.typing.ArrayType
import com.leko.kvm.typing.BooleanType
import com.leko.kvm.typing.ByteType
import com.leko.kvm.typing.CharType
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.DoubleType
import com.leko.kvm.typing.FloatType
import com.leko.kvm.typing.IntType
import com.leko.kvm.typing.LongType
import com.leko.kvm.typing.NullType
import com.leko.kvm.typing.ReferenceType
import com.leko.kvm.typing.ShortType
import com.leko.kvm.typing.Type
import com.leko.kvm.typing.VoidType

/**
 * Receives generated JVM bytecode instructions and associated structural
 * information.
 *
 * Implementations can use the emitted instructions to construct a class-file,
 * an intermediate representation, or another bytecode representation.
 */
interface InstructionGenerator {

    /**
     * Emits an instruction.
     *
     * @param instruction instruction to emit.
     */
    fun emit(instruction: Instruction)

    /**
     * Places a label at the current position in the generated instruction stream.
     *
     * @param label label identifying the current bytecode position.
     */
    fun placeLabel(label: Label)

    /**
     * Registers an exception handler for a bytecode range.
     *
     * @param start first label of the protected region.
     * @param end label marking the end of the protected region.
     * @param handler label at which the handler begins.
     * @param type exception type handled by the handler, or `null` for a
     * catch-all handler.
     */
    fun registerExceptionHandler(start: Label, end: Label, handler: Label, type: ClassType?)
}


/**
 * A node that can generate JVM bytecode through an [InstructionGenerator].
 *
 * Implementations describe either a value or a statement in the bytecode
 * generation tree.
 */
interface JBCTree {

    /**
     * Generates bytecode for this tree.
     *
     * @param generator target receiving generated instructions and labels.
     */
    fun generate(generator: InstructionGenerator)
}

/**
 * A bytecode-generation tree that produces a value.
 *
 * Values expose their JVM-level type and can be used as operands of expressions,
 * method calls, field accesses, control-flow conditions, and other operations.
 */
interface Value : JBCTree {

    /**
     * Type of the value produced by this tree.
     */
    val type: Type
}

/**
 * A value that identifies a storage location rather than directly producing
 * its stored value.
 *
 * Pointers can be used as targets of [Store] operations and may represent
 * locals, fields, or array elements.
 */
sealed interface Ptr : Value

/**
 * A pointer to a local variable slot.
 *
 * @property address local-variable slot used by the JVM bytecode.
 * @property type type of the local variable.
 */
data class LocalPtr(
    val address: Int,
    override val type: Type,
) : Ptr {
    override fun generate(generator: InstructionGenerator) {
        generator.emit(VarLoadInstruction(type, address))
    }
}

/**
 * A pointer to an instance field.
 *
 * @property owner class declaring the field.
 * @property name field name.
 * @property type field type.
 */
data class FieldPtr(
    val owner: ClassType,
    val name: String,
    override val type: Type,
): Ptr {
    override fun generate(generator: InstructionGenerator) {
        generator.emit(GetFieldInstruction(FieldSignature(owner, name, type)))
    }
}

/**
 * A pointer to a static field.
 *
 * @property clazz class declaring the field.
 * @property name field name.
 * @property type field type.
 */
data class StaticPtr(
    val clazz: ClassType,
    val name: String,
    override val type: Type,
): Ptr {
    override fun generate(generator: InstructionGenerator) {
        generator.emit(GetStaticInstruction(FieldSignature(clazz, name, type)))
    }
}

/**
 * A pointer to an element of an array.
 *
 * The type of this pointer is the component type of [array].
 *
 * @property array array containing the element.
 * @property index index of the element.
 */
data class ArrayPtr(val array: Value, val index: Value) : Ptr {
    override val type = (array.type as ArrayType).generic
    override fun generate(generator: InstructionGenerator) {
        array.generate(generator)
        index.generate(generator)
        val op = when (type) {
            IntType -> IALoadInstruction
            LongType -> LALoadInstruction
            FloatType -> FALoadInstruction
            DoubleType -> DALoadInstruction
            ByteType, BooleanType -> BALoadInstruction
            CharType -> CALoadInstruction
            ShortType -> SALoadInstruction
            else -> AALoadInstruction
        }
        generator.emit(op)
    }
}

/**
 * Allocates a new array.
 *
 * @property elementType type of each array element.
 * @property size number of elements to allocate.
 */
data class NewArray(val elementType: Type, val size: Value) : Value {
    override val type = ArrayType(elementType)
    override fun generate(generator: InstructionGenerator) {
        size.generate(generator)
        generator.emit(NewArrayInstruction(elementType, 1))
    }
}

/**
 * Reads the length of an array.
 *
 * The resulting value has type [IntType].
 *
 * @property array array whose length is read.
 */
data class ArrayLength(val array: Value) : Value {
    override val type = IntType
    override fun generate(generator: InstructionGenerator) {
        array.generate(generator)
        generator.emit(ArrayLengthInstruction)
    }
}

/**
 * Performs a JVM reference cast using `CHECKCAST`.
 *
 * @property value reference to cast.
 * @property type target reference type.
 */
data class Cast(val value: Value, override val type: ReferenceType) : Value {
    override fun generate(generator: InstructionGenerator) {
        value.generate(generator)
        generator.emit(CheckCastInstruction(type))
    }
}

/**
 * Performs a primitive numeric conversion.
 *
 * The supported conversions correspond to JVM primitive conversion
 * instructions such as `I2L`, `L2I`, `F2D`, and `D2F`.
 *
 * @property value value to convert.
 * @property type target primitive type.
 */
data class PrimitiveCast(val value: Value, override val type: Type) : Value {
    override fun generate(generator: InstructionGenerator) {
        value.generate(generator)
        val instruction: Instruction = when(value.type) {
            is IntType -> {
                when (type) {
                    LongType -> I2LInstruction
                    FloatType -> I2FInstruction
                    DoubleType -> I2DInstruction
                    else -> throw IllegalStateException()
                }
            }

            is LongType -> {
                when (type) {
                    IntType -> L2IInstruction
                    FloatType -> L2FInstruction
                    DoubleType -> L2DInstruction
                    else -> throw IllegalStateException()
                }
            }

            is FloatType -> {
                when (type) {
                    IntType -> F2IInstruction
                    LongType -> F2LInstruction
                    DoubleType -> F2DInstruction
                    else -> throw IllegalStateException()
                }
            }

            is DoubleType -> {
                when (type) {
                    IntType -> D2IInstruction
                    LongType -> D2LInstruction
                    FloatType -> D2FInstruction
                    else -> throw IllegalStateException()
                }
            }

            else -> {
                throw IllegalStateException()
            }
        }
        generator.emit(instruction)
    }
}

/**
 * Represents the JVM `null` reference.
 */
data object NullValue : Value {
    override val type: Type = NullType
    override fun generate(generator: InstructionGenerator) {
        generator.emit(AConstNullInstruction)
    }
}


/**
 * A constant byte value.
 *
 * @property value byte value represented by this node.
 */
data class ByteValue(val value: Byte) : Value {
    override val type: Type = ByteType
    override fun generate(generator: InstructionGenerator) = generator.emit(IntConstant(value.toInt()))
}

/**
 * A constant character value.
 *
 * @property value character represented by this node.
 */
data class CharValue(val value: Char) : Value {
    override val type: Type = CharType
    override fun generate(generator: InstructionGenerator) = generator.emit(IntConstant(value.code))
}

/**
 * A constant short value.
 *
 * @property value short value represented by this node.
 */
data class ShortValue(val value: Short) : Value {
    override val type: Type = ShortType
    override fun generate(generator: InstructionGenerator) = generator.emit(IntConstant(value.toInt()))
}

/**
 * Constant integer value.
 */
data class IntValue(val value: Int) : Value {
    override val type: Type = IntType
    override fun generate(generator: InstructionGenerator) = generator.emit(IntConstant(value))
}

/**
 * Constant long value.
 */
data class LongValue(val value: Long) : Value {
    override val type: Type = LongType
    override fun generate(generator: InstructionGenerator) = generator.emit(LongConstant(value))
}

/**
 * Constant floating-point value.
 */
data class FloatValue(val value: Float) : Value {
    override val type: Type = FloatType
    override fun generate(generator: InstructionGenerator) = generator.emit(FloatConstant(value))
}

/**
 * Constant double value.
 */
data class DoubleValue(val value: Double) : Value {
    override val type: Type = DoubleType
    override fun generate(generator: InstructionGenerator) = generator.emit(DoubleConstant(value))
}

/**
 * Constant boolean value.
 */
data class BooleanValue(val value: Boolean) : Value {
    override val type: Type = BooleanType
    override fun generate(generator: InstructionGenerator) {
        generator.emit(if (value) IConst1Instruction else IConst0Instruction)
    }
}

/**
 * Constant string value.
 */
data class StringValue(val value: String) : Value {
    override val type: Type get() = ClassType("java.lang.String")
    override fun generate(generator: InstructionGenerator) = generator.emit(LdcInstruction(value))
}

/**
 * Tests whether the supplied value is an instance of the requested reference type.
 */
data class InstanceOf(
    val value: Value,
    val checkedType: ReferenceType
): Value {
    override val type: Type = BooleanType
    override fun generate(generator: InstructionGenerator) {
        value.generate(generator)
        generator.emit(DupInstruction)
        generator.emit(InstanceOfInstruction(checkedType))
    }
}

/**
 * Binary arithmetic or comparison operation over two values.
 */
data class Operation(
    val lhs: Value,
    val rhs: Value,
    val op: ArithmeticInstruction,
    override val type: Type
): Value {
    override fun generate(generator: InstructionGenerator) {
        lhs.generate(generator)
        rhs.generate(generator)
        generator.emit(op)
    }
}

/**
 * Unary arithmetic or comparison operation over a single value.
 */
data class UnaryOperation(
    val operand: Value,
    val op: ArithmeticInstruction,
    override val type: Type
) : Value {
    override fun generate(generator: InstructionGenerator) {
        operand.generate(generator)
        generator.emit(op)
    }
}

/**
 * Virtual method invocation on an instance receiver.
 */
data class CallVirtual(
    val receiver: Value?,
    val owner: ClassType,
    val name: String,
    val args: List<Value>,
    val returnType: Type
): Value {
    override val type: Type get() = returnType
    override fun generate(generator: InstructionGenerator) {
        receiver?.generate(generator)
        args.forEach { it.generate(generator) }
        val sig = MethodSignature(name, MethodDescriptor(args.map { it.type }, returnType))
        generator.emit(InvokeVirtualInstruction(owner, sig))
    }
}

/**
 * Invokes a special method, such as a constructor or private method call.
 */
data class CallSpecial(
    val receiver: Value?,
    val owner: ClassType,
    val name: String,
    val args: List<Value>,
    val returnType: Type
): Value {
    override val type: Type get() = returnType
    override fun generate(generator: InstructionGenerator) {
        receiver?.generate(generator)
        args.forEach { it.generate(generator) }
        val sig = MethodSignature(name, MethodDescriptor(args.map { it.type }, returnType))
        generator.emit(InvokeSpecialInstruction(owner, sig))
    }
}

/**
 * Static method invocation on a class.
 */
data class StaticCall(
    val owner: ClassType,
    val name: String,
    val args: List<Value>,
    val returnType: Type,
): Value {
    override val type: Type get() = returnType
    override fun generate(generator: InstructionGenerator) {
        args.forEach { it.generate(generator) }
        val sig = MethodSignature(name, MethodDescriptor(args.map { it.type }, returnType))
        generator.emit(InvokeStaticInstruction(owner, sig))
    }
}

/**
 * Allocates a new object instance and invokes the matching constructor.
 */
data class New(
    override val type: ClassType,
    val args: List<Value>,
): Value {
    override fun generate(generator: InstructionGenerator) {
        // NEW, DUP, push args, INVOKESPECIAL <init>
        generator.emit(NewInstruction(type))
        generator.emit(DupInstruction)
        args.forEach { it.generate(generator) }
        val sig = MethodSignature("<init>", MethodDescriptor(args.map { it.type }, VoidType))
        generator.emit(InvokeSpecialInstruction(type, sig))
    }
}

/**
 * A statement in the bytecode-generation tree.
 *
 * Statements are side-effectful operations such as evaluations, stores,
 * branches, or exception handler registrations.
 */
interface Statement: JBCTree

/**
 * Wraps a raw bytecode instruction as a statement.
 */
data class Inst(val instruction: Instruction): Statement {
    override fun generate(generator: InstructionGenerator) {
        generator.emit(instruction)
    }
}

/**
 * Emits a source line-number mapping for debugging and stack traces.
 */
data class LineNumber(val line: Int) : Statement {
    override fun generate(generator: InstructionGenerator) = generator.emit(LineNumberInstruction(line))
}

/**
 * Evaluates a value and discards it from the stack.
 */
data class Pop(val value: Value) : Statement {
    override fun generate(generator: InstructionGenerator) {
        value.generate(generator)
        generator.emit(PopInstruction)
    }
}

/**
 * Evaluates a value without consuming it from the stack.
 */
data class Eval(val value: Value) : Statement {
    override fun generate(generator: InstructionGenerator) = value.generate(generator)
}

/**
 * Places a label at the current instruction position.
 */
data class LabelInst(val label: Label): Statement {
    override fun generate(generator: InstructionGenerator) = generator.placeLabel(label)
}

/**
 * Unconditionally jumps to the specified label.
 */
data class Goto(val label: Label): Statement {
    override fun generate(generator: InstructionGenerator) = generator.emit(GotoInstruction(label))
}

/**
 * Emits a branch instruction whose behavior depends on a runtime condition.
 */
data class Branch(val condition: Value, val instruction: () -> JumpInstruction) : Statement {
    override fun generate(generator: InstructionGenerator) {
        condition.generate(generator)
        generator.emit(instruction.invoke())
    }
}

/**
 * Returns a value or `void` from the current method.
 */
data class Return(val value: Value?) : Statement {
    override fun generate(generator: InstructionGenerator) {
        value?.generate(generator)
        generator.emit(ReturnInstruction(value?.type ?: VoidType))
    }
}

/**
 * Emits an integer-based switch dispatch.
 */
data class SwitchDispatch(
    val value: Value,
    val cases: Map<Int, Label>,
    val defaultLabel: Label,
) : Statement {
    override fun generate(generator: InstructionGenerator) {
        value.generate(generator)
        generator.emit(SwitchInstruction(defaultLabel, cases))
    }
}

/**
 * Sequencing node that emits several statements in order.
 */
data class Sequence(val statements: List<Statement>) : Statement {
    override fun generate(generator: InstructionGenerator) {
        for (statement in statements) {
            statement.generate(generator)
        }
    }
}

/**
 * Stores a value into a local, static field, instance field, or array slot.
 */
data class Store(val ptr: Ptr, val value: Value) : Statement {
    override fun generate(generator: InstructionGenerator) {
        when (ptr) {
            is LocalPtr -> {
                value.generate(generator)
                generator.emit(VarStoreInstruction(ptr.type, ptr.address))
            }
            is FieldPtr -> {
                // need `this` on stack for instance field — caller must push receiver first
                // here we assume receiver is already the first local (index 0)
                generator.emit(VarLoadInstruction(ptr.type, 0))
                value.generate(generator)
                generator.emit(PutFieldInstruction(FieldSignature(ptr.owner, ptr.name, ptr.type)))
            }
            is StaticPtr -> {
                value.generate(generator)
                generator.emit(PutStaticInstruction(FieldSignature(ptr.clazz, ptr.name, ptr.type)))
            }
            is ArrayPtr -> {
                ptr.array.generate(generator)
                ptr.index.generate(generator)
                value.generate(generator)
                generator.emit(ArrayStoreInstruction(value.type))
            }
        }
    }
}

/**
 * Registers an exception-handler range for a method body.
 */
data class ExceptionHandlerEntry(
    val start: Label,
    val end: Label,
    val handler: Label,
    val type: ClassType?,   // null = finally (catches everything)
) : Statement {
    override fun generate(generator: InstructionGenerator) {
        generator.registerExceptionHandler(start, end, handler, type)
    }
}

/**
 * Throws a throwable value from the current execution point.
 */
data class Throw(val throwable: Value): Statement {
    override fun generate(generator: InstructionGenerator) {
        throwable.generate(generator)
        generator.emit(AThrowInstruction)
    }
}
