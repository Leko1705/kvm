package com.leko.kvm.typing

sealed interface Type {

    val javaName: String

    val jvmName: String

    val internalName: String get() = jvmName

}