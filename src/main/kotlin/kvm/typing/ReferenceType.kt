package com.leko.kvm.typing

sealed interface ReferenceType: Type {

    override val javaName: String

    override val jvmName: String

}