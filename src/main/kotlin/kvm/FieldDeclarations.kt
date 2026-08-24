package com.leko.kvm

import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.Type

data class FieldSignature(
    val owner: ClassType,
    val name: String,
    val type: Type,
)

fun FieldSignature.readable(): String =
    "${owner.jvmName}.$name:${type.jvmName}"

sealed interface FieldDeclaration : Element {
    val signature: FieldSignature
    val accessFlags: FieldFlags
}

data class ConcreteField(
    override val signature: FieldSignature,
    override val accessFlags: FieldFlags,
    val annotations: List<KvmAnnotation>
) : FieldDeclaration

data class PhantomField(
    override val signature: FieldSignature,
    override val accessFlags: FieldFlags
) : FieldDeclaration