package com.leko.kvm.typing

data class ClassType(private val fqn: String): ReferenceType {
    companion object Companion
    override val javaName: String = fqn
    override val jvmName get() = "L${internalName};"
    override val internalName get() = fqn.replace('.', '/')
    override fun toString(): String = javaName
}