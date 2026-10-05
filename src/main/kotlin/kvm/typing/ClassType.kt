package com.leko.kvm.typing

/**
 * A class or interface type, identified by its fully qualified name.
 *
 * The name is expected in dotted form (`java.lang.String`). Nested classes use
 * the binary name with `$` (`java.util.Map$Entry`). Equality is by the exact
 * string given, so `ClassType("a.B")` and `ClassType("a/B")` are different types.
 *
 * @param fqn The fully qualified, dot-separated class name.
 */
data class ClassType(private val fqn: String): ReferenceType {
    /** Anchor for extension functions on `ClassType`. */
    companion object Companion

    /** The fully qualified name as given, such as `java.lang.String`. */
    override val javaName: String = fqn

    /** The field descriptor, such as `Ljava/lang/String;`. Computed on each access. */
    override val jvmName get() = "L${internalName};"

    /** The slash-separated name, such as `java/lang/String`. Computed on each access. */
    override val internalName get() = fqn.replace('.', '/')

    override fun toString(): String = javaName
}