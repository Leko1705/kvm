package com.leko.kvm.typing

/**
 * A JVM type: a primitive, `void`, or a reference type.
 *
 * Each type has three string forms:
 * - [javaName], the source-level name (`int`, `java.lang.String`, `int[]`).
 * - [jvmName], the field descriptor form (`I`, `Ljava/lang/String;`, `[I`).
 * - [internalName], the form used in class file structures and ASM APIs
 *   (`java/lang/String`, `[I`).
 *
 * The hierarchy is sealed, so `when` over a [Type] can be exhaustive.
 */
sealed interface Type {

    /** The source-level name, such as `int`, `java.lang.String` or `int[]`. */
    val javaName: String

    /** The JVM field descriptor, such as `I`, `Ljava/lang/String;` or `[I`. */
    val jvmName: String

    /**
     * The JVM internal name. For classes this is the slash-separated name without
     * the `L...;` wrapper (`java/lang/String`). For every other type it is the same
     * as [jvmName], which matches how the class file format names array classes.
     */
    val internalName: String get() = jvmName

}