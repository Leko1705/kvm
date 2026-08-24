@file:Suppress("UNUSED")
package com.leko.kvm.bytecode

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
import com.leko.kvm.typing.ShortType
import com.leko.kvm.typing.Type
import com.leko.kvm.typing.VoidType
import com.leko.kvm.typing.toType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


fun byte(byteValue: Byte): ByteValue = ByteValue(byteValue)

fun char(charValue: Char): CharValue = CharValue(charValue)

fun short(shortValue: Short): ShortValue = ShortValue(shortValue)

fun int(intValue: Int): Value = IntValue(intValue)

fun long(longValue: Long): Value = LongValue(longValue)

fun float(floatValue: Float): Value = FloatValue(floatValue)

fun double(doubleValue: Double): Value = DoubleValue(doubleValue)

fun bool(booleanValue: Boolean): Value = BooleanValue(booleanValue)

fun string(s: String): Value = StringValue(s)

operator fun Value.plus(other: Value): Value {
    if (this.type == IntType && other.type == IntType) {
        return Operation(this, other, IAddInstruction, IntType)
    }
    if (this.type == LongType && other.type == LongType) {
        return Operation(this, other, LAddInstruction, LongType)
    }
    if (this.type == FloatType && other.type == FloatType) {
        return Operation(this, other, FAddInstruction, FloatType)
    }
    if (this.type == DoubleType && other.type == DoubleType) {
        return Operation(this, other, DAddInstruction, DoubleType)
    }
    throw IllegalArgumentException("can not sum ${this.type} and ${other.type}")
}

operator fun Value.minus(other: Value): Value {
    if (this.type == IntType && other.type == IntType) {
        return Operation(this, other, ISubInstruction, IntType)
    }
    if (this.type == LongType && other.type == LongType) {
        return Operation(this, other, LSubInstruction, LongType)
    }
    if (this.type == FloatType && other.type == FloatType) {
        return Operation(this, other, FSubInstruction, FloatType)
    }
    if (this.type == DoubleType && other.type == DoubleType) {
        return Operation(this, other, DSubInstruction, DoubleType)
    }
    throw IllegalArgumentException("can not subtract $other from $this")
}

operator fun Value.times(other: Value): Value {
    if (this.type == IntType && other.type == IntType) {
        return Operation(this, other, IMulInstruction, IntType)
    }
    if (this.type == LongType && other.type == LongType) {
        return Operation(this, other, LMulInstruction, LongType)
    }
    if (this.type == FloatType && other.type == FloatType) {
        return Operation(this, other, FMulInstruction, FloatType)
    }
    if (this.type == DoubleType && other.type == DoubleType) {
        return Operation(this, other, DMulInstruction, DoubleType)
    }
    throw IllegalArgumentException("can not multiply $this by $other")
}

operator fun Value.div(other: Value): Value {
    if (this.type == IntType && other.type == IntType) {
        return Operation(this, other, IDivInstruction, IntType)
    }
    if (this.type == LongType && other.type == LongType) {
        return Operation(this, other, LDivInstruction, IntType)
    }
    if (this.type == FloatType && other.type == FloatType) {
        return Operation(this, other, FDivInstruction, FloatType)
    }
    if (this.type == DoubleType && other.type == DoubleType) {
        return Operation(this, other, DDivInstruction, DoubleType)
    }
    throw IllegalArgumentException("can not divide $this by $other")
}

infix fun Value.and(other: Value): Value {
    if (this.type == IntType && other.type == IntType) {
        return Operation(this, other, IAndInstruction, IntType)
    }
    if (this.type == LongType && other.type == LongType) {
        return Operation(this, other, LAndInstruction, LongType)
    }
    if (this.type == BooleanType && other.type == BooleanType) {
        return Operation(this, other, IAndInstruction, BooleanType)
    }
    throw IllegalArgumentException("can not 'and' $this with $other")
}

infix fun Value.or(other: Value): Value {
    if (this.type == IntType && other.type == IntType) {
        return Operation(this, other, IOrInstruction, IntType)
    }
    if (this.type == LongType && other.type == LongType) {
        return Operation(this, other, LOrInstruction, LongType)
    }
    if (this.type == BooleanType && other.type == BooleanType) {
        return Operation(this, other, IOrInstruction, BooleanType)
    }
    throw IllegalArgumentException("can not 'or' $this with $other")
}

operator fun Value.not(): Value = when (this.type) {
    IntType -> UnaryOperation(this, INegInstruction, IntType)
    LongType -> UnaryOperation(this, LNegInstruction, LongType)
    FloatType -> UnaryOperation(this, FNegInstruction, FloatType)
    DoubleType -> UnaryOperation(this, DNegInstruction, DoubleType)
    BooleanType -> Operation(this, IntValue(1), IXorInstruction, BooleanType)
    else -> throw IllegalArgumentException("can not invert $this")
}

operator fun Value.get(index: Value): Ptr {
    if (this.type !is ArrayType) {
        throw IllegalArgumentException("Type should be ArrayType")
    }
    return ArrayPtr(this, index)
}

operator fun LocalPtr.getValue(thisRef: Any?, property: KProperty<*>): Value = this

fun Value.field(name: String, type: Type): FieldPtr {
    val owner = this.type
    if (owner !is ClassType) {
        throw IllegalArgumentException("Owner must be a ClassType")
    }
    return FieldPtr(owner, name, type)
}

fun Value.field(name: String, type: KClass<*>): FieldPtr = field(name, type.toType())

fun ClassType.field(name: String, type: Type): Ptr = StaticPtr(this, name, type)

fun ClassType.field(name: String, type: KClass<*>): Ptr = field(name, type.toType())

fun KClass<*>.field(name: String, type: Type): Ptr {
    val ty = this.toType()
    if (ty !is ClassType) {
        throw IllegalArgumentException("Type should be a ClassType; primitives have no fields")
    }
    return ty.field(name, type)
}

fun KClass<*>.field(name: String, type: KClass<*>): Ptr {
    val ty = this.toType()
    if (ty !is ClassType) {
        throw IllegalArgumentException("Type should be a ClassType; primitives have no fields")
    }
    return ty.field(name, type)
}


fun Value.call(name: String): Call =
    Call(
        this,
        MethodSignature(
            this.type as ClassType,
            name,
            emptyList(),
            VoidType,
        ),
        emptyList(),
    )

fun Call.withArgs(vararg args: Value): Call = this.copy(args = args.toList())

fun Call.returns(type: Type): Call = this.copy(signature = signature.copy(returnType = type))

fun Call.returns(type: KClass<*>): Call = returns(type.toType())

fun ClassType.call(name: String): StaticCall = StaticCall(this, name, emptyList(), VoidType)

fun KClass<*>.call(name: String): StaticCall = (this.toType() as ClassType).call(name)

fun StaticCall.withArgs(vararg args: Value): StaticCall = this.copy(args = args.toList())

fun StaticCall.returns(type: Type): StaticCall = this.copy(returnType = type)

fun StaticCall.returns(type: KClass<*>): StaticCall = returns(type.toType())

fun ClassType.new(): New = New(this, emptyList())

fun KClass<*>.new(): New {
    val ty = this.toType()
    if (ty !is ClassType) {
        throw IllegalArgumentException("Class type must be a ClassType; primitives cannot get instantiated")
    }
    return ty.new()
}

fun New.withArgs(vararg args: Value): New = this.copy(args = args.toList())

fun newArray(elementType: Type, size: Value): Value = NewArray(elementType, size)

fun newArray(elementType: KClass<*>, size: Value): Value = newArray(elementType.toType(), size)

fun newArray(elementType: Type, size: Int): Value = newArray(elementType, int(size))

fun newArray(elementType: KClass<*>, size: Int): Value = newArray(elementType.toType(), size)

val Value.length: Value get() {
    if (this.type !is ArrayType) {
        throw IllegalArgumentException("$this is not an array")
    }
    return ArrayLength(this)
}

fun Value.toInt(): Value = PrimitiveCast(this, IntType)

fun Value.toLong(): Value = PrimitiveCast(this, LongType)

fun Value.toFloat(): Value = PrimitiveCast(this, FloatType)

fun Value.toDouble(): Value = PrimitiveCast(this, DoubleType)

fun Value.toByte(): Value = PrimitiveCast(this, ByteType)

fun Value.toChar(): Value = PrimitiveCast(this, CharType)

fun Value.toShort(): Value = PrimitiveCast(this, ShortType)

infix fun Value.into(to: ClassType): Value = Cast(this, to)

fun Value.isInstanceOf(type: ClassType): Value = InstanceOf(this, type)

infix fun Value.eq(other: Value): Value = TODO() //Equals(other, this)

infix fun Value.neq(other: Value): Value = TODO() //NotEquals(other, this)

infix fun Value.lt(other: Value): Value = TODO() //LessThan(other)

infix fun Value.gt(other: Value): Value = TODO() //GreaterThan(other)

infix fun Value.geq(other: Value): Value = TODO() //GreaterOrEqualThan(other)

infix fun Value.leq(other: Value): Value = TODO() //LessOrEqualThan(other)

