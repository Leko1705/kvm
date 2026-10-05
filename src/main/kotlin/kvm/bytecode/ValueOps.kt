@file:Suppress("UNUSED")
package com.leko.kvm.bytecode

import com.leko.kvm.typing.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/** Converts a primitive byte value to a `Value` object */
fun byte(byteValue: Byte): ByteValue = ByteValue(byteValue)

/** Converts a primitive char value to a `Value` object */
fun char(charValue: Char): CharValue = CharValue(charValue)

/** Converts a primitive short value to a `Value` object */
fun short(shortValue: Short): ShortValue = ShortValue(shortValue)

/** Converts a primitive int value to a `Value` object */
fun int(intValue: Int): Value = IntValue(intValue)

/** Converts a primitive long value to a `Value` object */
fun long(longValue: Long): Value = LongValue(longValue)

/** Converts a primitive float value to a `Value` object */
fun float(floatValue: Float): Value = FloatValue(floatValue)

/** Converts a primitive double value to a `Value` object */
fun double(doubleValue: Double): Value = DoubleValue(doubleValue)

/** Converts a primitive boolean value to a `Value` object */
fun bool(booleanValue: Boolean): Value = BooleanValue(booleanValue)

/** Converts a primitive string value to a `Value` object */
fun string(s: String): Value = StringValue(s)

/**
 * Adds two numeric values.
 *
 * Both operands must have the same supported primitive type:
 * `int`, `long`, `float`, or `double`.
 *
 * @throws IllegalArgumentException if the operand types are unsupported or
 * incompatible.
 */
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

/**
 * Subtracts [other] from this value.
 *
 * Supported operand types are `int`, `long`, `float`, and `double`.
 *
 * @throws IllegalArgumentException if the operand types are unsupported or
 * incompatible.
 */
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

/**
 * Multiplies two numeric values.
 *
 * Supported operand types are `int`, `long`, `float`, and `double`.
 *
 * @throws IllegalArgumentException if the operand types are unsupported or
 * incompatible.
 */
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

/**
 * Divides this value by [other].
 *
 * Supported operand types are `int`, `long`, `float`, and `double`.
 *
 * @throws IllegalArgumentException if the operand types are unsupported or
 * incompatible.
 */
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

/**
 * Computes the remainder of this value divided by [other].
 *
 * Supported operand types are `int`, `long`, `float`, and `double`.
 *
 * @throws IllegalArgumentException if the operand types are unsupported or
 * incompatible.
 */
operator fun Value.rem(other: Value): Value {
    if (this.type == IntType && other.type == IntType) {
        return Operation(this, other, IRemInstruction, IntType)
    }
    if (this.type == LongType && other.type == LongType) {
        return Operation(this, other, LRemInstruction, LongType)
    }
    if (this.type == FloatType && other.type == FloatType) {
        return Operation(this, other, FRemInstruction, FloatType)
    }
    if (this.type == DoubleType && other.type == DoubleType) {
        return Operation(this, other, DRemInstruction, DoubleType)
    }
    throw IllegalArgumentException("can not 'mod' $this by $other")
}

/**
 * Performs a bitwise AND operation.
 *
 * Supported types are `int`, `long`, and `boolean`.
 *
 * @throws IllegalArgumentException if the operands have an unsupported type
 * or incompatible types.
 */
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

/**
 * Performs a bitwise OR operation.
 *
 * Supported types are `int`, `long`, and `boolean`.
 *
 * @throws IllegalArgumentException if the operands have an unsupported type
 * or incompatible types.
 */
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

/**
 * Negates a numeric value or inverts a boolean value.
 *
 * For booleans this operation is implemented using XOR with `1`.
 *
 * @throws IllegalArgumentException if the value has an unsupported type.
 */
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

/**
 * Creates a pointer to an instance field of this value.
 *
 * The receiver must have a [ClassType].
 *
 * @param name field name.
 * @param type field type.
 * @return pointer representing the field.
 *
 * @throws IllegalArgumentException if this value is not a class reference.
 */
fun Value.field(name: String, type: Type): FieldPtr {
    val owner = this.type
    if (owner !is ClassType) {
        throw IllegalArgumentException("Owner must be a ClassType")
    }
    return FieldPtr(owner, name, type)
}

/**
 * Creates a pointer to an instance field using a Kotlin class as its type.
 */
fun Value.field(name: String, type: KClass<*>): FieldPtr = field(name, type.toType())

/**
 * Creates a pointer to a static field of this class.
 *
 * @param name field name.
 * @param type field type.
 * @return pointer representing the static field.
 */
fun ClassType.field(name: String, type: Type): Ptr = StaticPtr(this, name, type)

/**
 * Creates a pointer to a static field using a Kotlin class as its type.
 */
fun ClassType.field(name: String, type: KClass<*>): Ptr = field(name, type.toType())

/**
 * Creates a pointer to a static field of the represented JVM class.
 *
 * @throws IllegalArgumentException if this Kotlin class represents a primitive
 * type rather than a JVM class type.
 */
fun KClass<*>.field(name: String, type: Type): Ptr {
    val ty = this.toType()
    if (ty !is ClassType) {
        throw IllegalArgumentException("Type should be a ClassType; primitives have no fields")
    }
    return ty.field(name, type)
}

/**
 * Creates a pointer to a static field using Kotlin classes for both the owner
 * and field type.
 */
fun KClass<*>.field(name: String, type: KClass<*>): Ptr {
    val ty = this.toType()
    if (ty !is ClassType) {
        throw IllegalArgumentException("Type should be a ClassType; primitives have no fields")
    }
    return ty.field(name, type)
}

/**
 * Creates a virtual call on this value.
 *
 * The call initially has no arguments and a `void` return type. Use
 * [CallVirtual.withArgs] and [CallVirtual.returns] to configure it.
 *
 * @param name method name.
 * @return virtual method call expression.
 */
fun Value.call(name: String): CallVirtual =
    CallVirtual(
        this,
        this.type as ClassType,
        name,
        emptyList(),
        VoidType,
    )

/**
 * Adds arguments to a virtual method call.
 *
 * @param args arguments in invocation order.
 * @return a copy of this call with the supplied arguments.
 */
fun CallVirtual.withArgs(vararg args: Value): CallVirtual = this.copy(args = args.toList())

/**
 * Sets the return type of the virtual method call.
 *
 * @param type return type.
 * @return a copy of this call with the supplied return type.
 */
fun CallVirtual.returns(type: Type): CallVirtual = this.copy(returnType = type)

/**
 * Sets the return type of the virtual method call using a Kotlin class.
 */
fun CallVirtual.returns(type: KClass<*>): CallVirtual = returns(type.toType())

/**
 * Creates a static method call on this class.
 *
 * The call initially has no arguments and a `void` return type.
 *
 * @param name method name.
 * @return static method call expression.
 */
fun ClassType.call(name: String): StaticCall = StaticCall(this, name, emptyList(), VoidType)

/**
 * Creates a static method call on the JVM class represented by this Kotlin class.
 */
fun KClass<*>.call(name: String): StaticCall = (this.toType() as ClassType).call(name)

/**
 * Adds arguments to a static method call.
 */
fun StaticCall.withArgs(vararg args: Value): StaticCall = this.copy(args = args.toList())

/**
 * Sets the return type of the static method call.
 */
fun StaticCall.returns(type: Type): StaticCall = this.copy(returnType = type)

/**
 * Sets the return type of the static method call using a Kotlin class.
 */
fun StaticCall.returns(type: KClass<*>): StaticCall = returns(type.toType())

/**
 * Creates an object-construction expression for this class.
 *
 * The resulting expression initially has no constructor arguments.
 */
fun ClassType.new(): New = New(this, emptyList())

/**
 * Creates an object-construction expression for the JVM class represented
 * by this Kotlin class.
 *
 * @throws IllegalArgumentException if this class represents a primitive type.
 */
fun KClass<*>.new(): New {
    val ty = this.toType()
    if (ty !is ClassType) {
        throw IllegalArgumentException("Class type must be a ClassType; primitives cannot get instantiated")
    }
    return ty.new()
}

/**
 * Adds constructor arguments to an object-construction expression.
 *
 * @param args constructor arguments in declaration order.
 * @return a copy with the supplied arguments.
 */
fun New.withArgs(vararg args: Value): New = this.copy(args = args.toList())

/**
 * Creates an array allocation expression.
 *
 * @param elementType array component type.
 * @param size number of elements.
 */
fun newArray(elementType: Type, size: Value): Value = NewArray(elementType, size)

/**
 * Creates an array allocation expression using a Kotlin class as its element type.
 */
fun newArray(elementType: KClass<*>, size: Value): Value = newArray(elementType.toType(), size)

/**
 * Creates an array allocation expression with a constant size.
 */
fun newArray(elementType: Type, size: Int): Value = newArray(elementType, int(size))

/**
 * Creates an array allocation expression using a Kotlin class and constant size.
 */
fun newArray(elementType: KClass<*>, size: Int): Value = newArray(elementType.toType(), size)

/**
 * Returns an expression representing the length of this array.
 *
 * @throws IllegalArgumentException if this value is not an array.
 */
val Value.length: Value get() {
    if (this.type !is ArrayType) {
        throw IllegalArgumentException("$this is not an array")
    }
    return ArrayLength(this)
}

/**
 * Converts this value to `int`.
 */
fun Value.toInt(): Value = PrimitiveCast(this, IntType)

/**
 * Converts this value to `long`.
 */
fun Value.toLong(): Value = PrimitiveCast(this, LongType)

/**
 * Converts this value to `float`.
 */
fun Value.toFloat(): Value = PrimitiveCast(this, FloatType)

/**
 * Converts this value to `double`.
 */
fun Value.toDouble(): Value = PrimitiveCast(this, DoubleType)

/**
 * Converts this value to `byte`.
 */
fun Value.toByte(): Value = PrimitiveCast(this, ByteType)

/**
 * Converts this value to `char`.
 */
fun Value.toChar(): Value = PrimitiveCast(this, CharType)

/**
 * Converts this value to `short`.
 */
fun Value.toShort(): Value = PrimitiveCast(this, ShortType)

/**
 * Casts this value to a reference type using JVM `CHECKCAST`.
 *
 * @param to target reference type.
 */
infix fun Value.into(to: ReferenceType): Value = Cast(this, to)

/**
 * Tests whether this value is an instance of the supplied reference type.
 */
fun Value.isInstanceOf(type: ReferenceType): Value = InstanceOf(this, type)

/**
 * Compares two values for equality.
 *
 * @return a boolean comparison value.
 *
 * @throws NotImplementedError comparison generation is not implemented yet.
 */
infix fun Value.eq(other: Value): Value = TODO() //Equals(other, this)

/**
 * Compares two values for inequality.
 *
 * @throws NotImplementedError comparison generation is not implemented yet.
 */
infix fun Value.neq(other: Value): Value = TODO() //NotEquals(other, this)

/**
 * Tests whether this value is less than [other].
 *
 * @throws NotImplementedError comparison generation is not implemented yet.
 */
infix fun Value.lt(other: Value): Value = TODO() //LessThan(other)

/**
 * Tests whether this value is greater than [other].
 *
 * @throws NotImplementedError comparison generation is not implemented yet.
 */
infix fun Value.gt(other: Value): Value = TODO() //GreaterThan(other)

/**
 * Tests whether this value is greater than or equal to [other].
 *
 * @throws NotImplementedError comparison generation is not implemented yet.
 */
infix fun Value.geq(other: Value): Value = TODO() //GreaterOrEqualThan(other)

/**
 * Tests whether this value is less than or equal to [other].
 *
 * @throws NotImplementedError comparison generation is not implemented yet.
 */
infix fun Value.leq(other: Value): Value = TODO() //LessOrEqualThan(other)

