package com.leko.kvm.typing


inline val Type.isPrimitive: Boolean get() = this !is ReferenceType

object VoidType : Type {
    override val javaName = "void"
    override val jvmName = "V"
}
object ByteType : Type {
    override val javaName = "byte"
    override val jvmName = "B"
    override fun toString(): String = javaName
}
object CharType : Type {
    override val javaName = "char"
    override val jvmName = "C"
    override fun toString(): String = javaName
}
object ShortType : Type {
    override val javaName = "short"
    override val jvmName = "S"
    override fun toString(): String = javaName
}
object IntType : Type {
    override val javaName = "int"
    override val jvmName = "I"
    override fun toString(): String = javaName
}
object LongType : Type {
    override val javaName = "long"
    override val jvmName = "J"
    override fun toString(): String = javaName
}
object FloatType : Type {
    override val javaName = "float"
    override val jvmName = "F"
    override fun toString(): String = javaName
}
object DoubleType : Type {
    override val javaName = "double"
    override val jvmName = "D"
    override fun toString(): String = javaName
}
object BooleanType : Type {
    override val javaName = "boolean"
    override val jvmName = "Z"
    override fun toString(): String = javaName
}
object NullType : ReferenceType {
    override val javaName: String = "null"
    override val jvmName: String = "Ljava/lang/Object;"
    override fun toString(): String = javaName
}
data class ArrayType(val generic: Type): ReferenceType {
    override val javaName = "${generic.javaName}[]"
    override val jvmName = "[${generic.jvmName}"
    override fun toString(): String = javaName
}
