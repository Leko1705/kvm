package com.leko.kvm.typing

import kotlin.reflect.KClass

/**
 * Parses a JVM field descriptor into a [Type].
 *
 * | Input                  | Result                    |
 * |------------------------|---------------------------|
 * | `I`                    | [IntType]                 |
 * | `V`                    | [VoidType]                |
 * | `Ljava/lang/String;`   | `ClassType("java.lang.String")` |
 * | `[I`                   | `ArrayType(IntType)`      |
 * | `[[Ljava/lang/Object;` | `ArrayType(ArrayType(ClassType("java.lang.Object")))` |
 *
 * Class names have their slashes converted to dots. This is the inverse of [Type.jvmName]
 * for every type except [NullType].
 *
 * Only single descriptors are accepted. Method descriptors such as `(I)V` are not.
 * Validation is shallow: for `L...;` the contents are not checked, so the result
 * can be a [ClassType] with a nonsensical name.
 *
 * @receiver The descriptor to parse.
 * @return The parsed type.
 * @throws IllegalArgumentException if the string is not a recognizable descriptor
 * (unknown leading character, a missing `;`, or an empty array element).
 * @see parseInternalName
 * @see parseJavaName
 */
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

/**
 * Parses a JVM internal name into a [Type].
 *
 * Internal names are what the class file format uses for class references, for example
 * the owner of a method call or the operand of `new`:
 *
 * | Input                  | Result                    |
 * |------------------------|---------------------------|
 * | `java/lang/String`     | `ClassType("java.lang.String")` |
 * | `java/util/Map$Entry`  | `ClassType("java.util.Map$Entry")` |
 * | `[I`                   | `ArrayType(IntType)`      |
 * | `[Ljava/lang/String;`  | `ArrayType(ClassType("java.lang.String"))` |
 *
 * Array classes are the one exception to "no `L...;` wrapper": their internal
 * name is their descriptor, so names starting with `[` are handled by [parseJvmName].
 *
 * Primitive types have no internal name, so a string like `I` is read as a class
 * called `I` in the default package, not as [IntType]. This is the inverse of
 * [Type.internalName] for [ClassType] and [ArrayType] only. For primitives,
 * [Type.internalName] returns the descriptor, which does not round-trip.
 *
 * @receiver The internal name to parse.
 * @return A [ClassType] or [ArrayType].
 * @throws IllegalArgumentException if the string is empty, or contains `.`, `;`
 * or a `[` that is not part of a leading array prefix.
 */
fun String.parseInternalName(): Type {
    if (startsWith('[')) return parseJvmName()
    require(isNotEmpty()) { "Illegal internal name: empty string" }
    require(none { it == '.' || it == ';' || it == '[' }) { "Illegal internal name: $this" }
    return ClassType(replace('/', '.'))
}

/**
 * Parses a Java source-style type name into a [Type].
 *
 * Accepts primitive names and `void`, dotted class names, and any number of
 * trailing `[]` pairs:
 *
 * | Input                | Result                    |
 * |----------------------|---------------------------|
 * | `int`                | [IntType]                 |
 * | `java.lang.String`   | `ClassType("java.lang.String")` |
 * | `int[][]`            | `ArrayType(ArrayType(IntType))` |
 *
 * Class names are validated by [parseClassType]. Generics and annotations are
 * not supported. `void[]` is not rejected.
 *
 * @receiver The Java type name.
 * @return The parsed type.
 * @throws IllegalArgumentException if a `]` has no matching `[`, or the class
 * name is invalid.
 * @see parseJvmName
 */
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

/**
 * Parses a dotted class name into a [ClassType], after checking its characters.
 *
 * @receiver The fully qualified class name.
 * @throws IllegalArgumentException if the name is empty or has other characters.
 */
fun String.parseClassType(): ClassType {
    if (!this.matches("[0-9a-zA-Z_.$]+".toRegex())) {
        throw IllegalArgumentException("Illegal type name: $this")
    }
    return ClassType(this)
}


/**
 * Maps a Kotlin class reference to the corresponding [Type].
 *
 * - Kotlin primitives and `Unit` map to the primitive types and [VoidType].
 * - Primitive array classes (`IntArray` and so on) and array classes map to [ArrayType].
 * - Anything else becomes a [ClassType] named by [KClass.qualifiedName].
 *
 * @receiver The class to convert.
 * @throws IllegalArgumentException if the class has no qualified name
 * (anonymous or local classes).
 */
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
