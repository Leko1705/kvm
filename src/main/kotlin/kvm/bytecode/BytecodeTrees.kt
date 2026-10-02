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
import com.leko.kvm.typing.ShortType
import com.leko.kvm.typing.Type
import com.leko.kvm.typing.VoidType

interface InstructionGenerator {
    fun emit(instruction: Instruction)
    fun placeLabel(label: Label)
    fun registerExceptionHandler(start: Label, end: Label, handler: Label, type: ClassType?)
}

interface JBCTree {
    fun generate(generator: InstructionGenerator)
}

interface Value: JBCTree {
    val type: Type
}

sealed interface Ptr: Value

data class LocalPtr(
    val address: Int,
    override val type: Type,
) : Ptr {
    override fun generate(generator: InstructionGenerator) {
        generator.emit(VarLoadInstruction(type, address))
    }
}

data class FieldPtr(
    val owner: ClassType,
    val name: String,
    override val type: Type,
): Ptr {
    override fun generate(generator: InstructionGenerator) {
        generator.emit(GetFieldInstruction(FieldSignature(owner, name, type)))
    }
}

data class StaticPtr(
    val clazz: ClassType,
    val name: String,
    override val type: Type,
): Ptr {
    override fun generate(generator: InstructionGenerator) {
        generator.emit(GetStaticInstruction(FieldSignature(clazz, name, type)))
    }
}

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

data class NewArray(val elementType: Type, val size: Value) : Value {
    override val type = ArrayType(elementType)
    override fun generate(generator: InstructionGenerator) {
        size.generate(generator)
        generator.emit(NewArrayInstruction(elementType, 1))
    }
}

data class ArrayLength(val array: Value) : Value {
    override val type = IntType
    override fun generate(generator: InstructionGenerator) {
        array.generate(generator)
        generator.emit(ArrayLengthInstruction)
    }
}

data class Cast(val value: Value, override val type: ClassType) : Value {
    override fun generate(generator: InstructionGenerator) {
        value.generate(generator)
        generator.emit(CheckCastInstruction(type))
    }
}

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

data object NullValue : Value {
    override val type: Type = NullType
    override fun generate(generator: InstructionGenerator) {
        generator.emit(AConstNullInstruction)
    }
}

data class ByteValue(val value: Byte) : Value {
    override val type: Type = ByteType
    override fun generate(generator: InstructionGenerator) = generator.emit(IntConstant(value.toInt()))
}

data class CharValue(val value: Char) : Value {
    override val type: Type = CharType
    override fun generate(generator: InstructionGenerator) = generator.emit(IntConstant(value.code))
}

data class ShortValue(val value: Short) : Value {
    override val type: Type = ShortType
    override fun generate(generator: InstructionGenerator) = generator.emit(IntConstant(value.toInt()))
}

data class IntValue(val value: Int) : Value {
    override val type: Type = IntType
    override fun generate(generator: InstructionGenerator) = generator.emit(IntConstant(value))
}

data class LongValue(val value: Long) : Value {
    override val type: Type = LongType
    override fun generate(generator: InstructionGenerator) = generator.emit(LongConstant(value))
}

data class FloatValue(val value: Float) : Value {
    override val type: Type = FloatType
    override fun generate(generator: InstructionGenerator) = generator.emit(FloatConstant(value))
}

data class DoubleValue(val value: Double) : Value {
    override val type: Type = DoubleType
    override fun generate(generator: InstructionGenerator) = generator.emit(DoubleConstant(value))
}

data class BooleanValue(val value: Boolean) : Value {
    override val type: Type = BooleanType
    override fun generate(generator: InstructionGenerator) {
        generator.emit(if (value) IConst1Instruction else IConst0Instruction)
    }
}

data class StringValue(val value: String) : Value {
    override val type: Type get() = ClassType("java.lang.String")
    override fun generate(generator: InstructionGenerator) = generator.emit(LdcInstruction(value))
}

data class InstanceOf(
    val value: Value,
    val checkedType: ClassType
): Value {
    override val type: Type = BooleanType
    override fun generate(generator: InstructionGenerator) {
        value.generate(generator)
        generator.emit(DupInstruction)
        generator.emit(InstanceOfInstruction(checkedType))
    }
}

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

interface Statement: JBCTree

data class Inst(val instruction: Instruction): Statement {
    override fun generate(generator: InstructionGenerator) {
        generator.emit(instruction)
    }
}

data class LineNumber(val line: Int) : Statement {
    override fun generate(generator: InstructionGenerator) = generator.emit(LineNumberInstruction(line))
}

data class Pop(val value: Value) : Statement {
    override fun generate(generator: InstructionGenerator) {
        value.generate(generator)
        generator.emit(PopInstruction)
    }
}

data class Eval(val value: Value) : Statement {
    override fun generate(generator: InstructionGenerator) = value.generate(generator)
}

data class LabelInst(val label: Label): Statement {
    override fun generate(generator: InstructionGenerator) = generator.placeLabel(label)
}

data class Goto(val label: Label): Statement {
    override fun generate(generator: InstructionGenerator) = generator.emit(GotoInstruction(label))
}

data class Branch(val condition: Value, val instruction: () -> JumpInstruction) : Statement {
    override fun generate(generator: InstructionGenerator) {
        condition.generate(generator)
        generator.emit(instruction.invoke())
    }
}

data class Return(val value: Value?) : Statement {
    override fun generate(generator: InstructionGenerator) {
        value?.generate(generator)
        generator.emit(ReturnInstruction(value?.type ?: VoidType))
    }
}

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

data class Sequence(val statements: List<Statement>) : Statement {
    override fun generate(generator: InstructionGenerator) {
        for (statement in statements) {
            statement.generate(generator)
        }
    }
}

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

data class Throw(val throwable: Value): Statement {
    override fun generate(generator: InstructionGenerator) {
        throwable.generate(generator)
        generator.emit(AThrowInstruction)
    }
}
