package com.leko.kvm.typing

/**
 * Converts a Java-style type name to a JVM descriptor, for example
 * `java.lang.String` to `Ljava/lang/String;` and `int[]` to `[I`.
 *
 * @throws IllegalArgumentException if this is not a valid Java name.
 */
fun String.toJvmName(): String = parseJavaName().jvmName

/**
 * Converts a JVM descriptor to a Java-style type name, for example
 * `Ljava/lang/String;` to `java.lang.String` and `[I` to `int[]`.
 *
 * @throws IllegalArgumentException if this is not a valid descriptor.
 */
fun String.toJavaName(): String = parseJvmName().javaName

/** Whether this string is a valid JVM descriptor, as accepted by `parseJvmName`. */
fun String.isValidJvmName(): Boolean = try {
    parseJvmName()
    true
}
catch (_: IllegalArgumentException) {
    false
}

/** Whether this string is a valid Java-style type name, as accepted by `parseJavaName`. */
fun String.isValidJavaName(): Boolean = try {
    parseJavaName()
    true
}
catch (_: IllegalArgumentException) {
    false
}