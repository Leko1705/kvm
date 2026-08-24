package com.leko.kvm.typing


fun String.toJvmName(): String = parseJavaName().jvmName

fun String.toJavaName(): String = parseJvmName().javaName

fun String.isValidJvmName(): Boolean = try {
    parseJvmName()
    true
}
catch (_: IllegalArgumentException) {
    false
}

fun String.isValidJavaName(): Boolean = try {
    parseJavaName()
    true
}
catch (_: IllegalArgumentException) {
    false
}
