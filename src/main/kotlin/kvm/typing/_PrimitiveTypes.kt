package com.leko.kvm.typing

/**
 * `true` for every type that is not a [ReferenceType], which includes [VoidType].
 */
inline val Type.isPrimitive: Boolean get() = this !is ReferenceType

/** The `void` pseudo-type, valid only as a method return type. Descriptor `V`. */
object VoidType : Type {
    override val javaName = "void"
    override val jvmName = "V"
}

/** The primitive `byte`. Descriptor `B`. */
object ByteType : Type {
    override val javaName = "byte"
    override val jvmName = "B"
    override fun toString(): String = javaName
}

/** The primitive `char`. Descriptor `C`. */
object CharType : Type {
    override val javaName = "char"
    override val jvmName = "C"
    override fun toString(): String = javaName
}

/** The primitive `short`. Descriptor `S`. */
object ShortType : Type {
    override val javaName = "short"
    override val jvmName = "S"
    override fun toString(): String = javaName
}

/** The primitive `int`. Descriptor `I`. */
object IntType : Type {
    override val javaName = "int"
    override val jvmName = "I"
    override fun toString(): String = javaName
}

/** The primitive `long`. Descriptor `J`. Occupies two stack and local slots. */
object LongType : Type {
    override val javaName = "long"
    override val jvmName = "J"
    override fun toString(): String = javaName
}

/** The primitive `float`. Descriptor `F`. */
object FloatType : Type {
    override val javaName = "float"
    override val jvmName = "F"
    override fun toString(): String = javaName
}

/** The primitive `double`. Descriptor `D`. Occupies two stack and local slots. */
object DoubleType : Type {
    override val javaName = "double"
    override val jvmName = "D"
    override fun toString(): String = javaName
}

/** The primitive `boolean`. Descriptor `Z`. */
object BooleanType : Type {
    override val javaName = "boolean"
    override val jvmName = "Z"
    override fun toString(): String = javaName
}

/**
 * The type of the `null` literal, which is a subtype of every reference type.
 *
 * This is a pseudo-type for analysis. It never appears in a class file, and its
 * [jvmName] is only a stand-in (`Ljava/lang/Object;`).
 */
object NullType : ReferenceType {
    override val javaName: String = "null"
    override val jvmName: String = "Ljava/lang/Object;"
    override fun toString(): String = javaName
}

/**
 * An array type. Multidimensional arrays nest, so `int[][]` is
 * `ArrayType(ArrayType(IntType))`.
 *
 * @property generic The element type. Despite the name, this has nothing to do
 * with generics. `void` is not a valid element type but is not rejected.
 */
data class ArrayType(val generic: Type): ReferenceType {
    /** The element's [Type.javaName] followed by `[]`, such as `int[]`. */
    override val javaName = "${generic.javaName}[]"

    /** `[` followed by the element's descriptor, such as `[I` or `[Ljava/lang/String;`. */
    override val jvmName = "[${generic.jvmName}"

    override fun toString(): String = javaName
}