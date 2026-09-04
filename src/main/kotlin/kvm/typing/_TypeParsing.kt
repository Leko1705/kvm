package com.leko.kvm.typing

import kotlin.reflect.KClass


fun String.parseJvmName(): Type {
    val jvmName = this
    return when {
        jvmName == "V" -> VoidType
        jvmName == "B" -> ByteType
        jvmName == "C" -> CharType
        jvmName == "S" -> ShortType
        jvmName == "I" -> IntType
        jvmName == "J" -> LongType
        jvmName == "F" -> FloatType
        jvmName == "D" -> DoubleType
        jvmName == "Z" -> BooleanType
        jvmName.startsWith('[') -> {
            ArrayType(
                jvmName
                    .drop(1)
                    .parseJvmName()
            )
        }
        jvmName.startsWith('L') -> {
            if (!jvmName.endsWith(';')) {
                throw IllegalArgumentException("Invalid jvm name $jvmName; missing ';'")
            }
            ClassType(
                jvmName
                    .drop(1)
                    .dropLast(1)
                    .replace('/', '.')
            )
        }
        else -> throw IllegalArgumentException("Illegal jvm name: $this")
    }
}


fun String.parseJavaName(): Type {
    var jvmName = this
    var arrayDimension = 0

    while (jvmName.endsWith(']')) {
        if (!jvmName.endsWith("[]")) {
            throw IllegalArgumentException(
                "Illegal type name: $this; missing open bracket for array type")
        }
        jvmName = jvmName.dropLast(2)
        arrayDimension++
    }

    var type = when (jvmName) {
        "void" -> VoidType
        "byte" -> ByteType
        "char" -> CharType
        "short" -> ShortType
        "int" -> IntType
        "long" -> LongType
        "float" -> FloatType
        "double" -> DoubleType
        "boolean" -> BooleanType
        else -> jvmName.parseClassType()
    }

    repeat(arrayDimension) {
        type = ArrayType(type)
    }

    return type
}

fun String.parseClassType(): ClassType {
    if (!this.matches("[0-9a-zA-Z_.]+".toRegex())) {
        throw IllegalArgumentException("Illegal type name: $this")
    }
    return ClassType(this)
}

fun KClass<*>.toType(): Type {
    return when (this) {
        Unit::class -> VoidType
        Byte::class -> ByteType
        Char::class -> CharType
        Short::class -> ShortType
        Int::class -> IntType
        Long::class -> LongType
        Float::class -> FloatType
        Double::class -> DoubleType
        Boolean::class -> BooleanType;
        ByteArray::class -> ArrayType(ByteType)
        CharArray::class -> ArrayType(CharType)
        ShortArray::class -> ArrayType(ShortType)
        IntArray::class -> ArrayType(IntType)
        LongArray::class -> ArrayType(LongType)
        FloatArray::class -> ArrayType(FloatType)
        DoubleArray::class -> ArrayType(DoubleType)
        BooleanArray::class -> ArrayType(BooleanType)
        else -> {
            if (this.java.isArray) {
                val elementType = this.java.componentType.kotlin.toType()
                return ArrayType(elementType)
            }
            val fqn = this.qualifiedName ?: throw IllegalArgumentException("class has no name")
            ClassType(fqn)
        }
    }

}
