package com.leko.kvm

import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.Type

data class FieldSignature(
    val owner: ClassType,
    val name: String,
    val type: Type,
)

fun FieldSignature.readableJvm(): String =
    "${owner.jvmName}.$name:${type.jvmName}"

sealed interface FieldDeclaration : Element {
    val signature: FieldSignature
    val accessFlags: FieldFlags
}

val FieldDeclaration.owner: ClassType get() = signature.owner

val FieldDeclaration.name: String get() = signature.name

val FieldDeclaration.type: Type get() = signature.type

data class ConcreteField(
    override val signature: FieldSignature,
    override val accessFlags: FieldFlags,
    val annotations: List<KvmAnnotation>
) : FieldDeclaration

data class PhantomField(
    override val signature: FieldSignature,
    override val accessFlags: FieldFlags
) : FieldDeclaration