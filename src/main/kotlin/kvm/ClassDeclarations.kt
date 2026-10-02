package com.leko.kvm

import com.leko.kvm.typing.ClassType

sealed interface ClassDeclaration : Element {
    val type: ClassType
    val methods: List<MethodDeclaration>
    val fields: List<FieldDeclaration>
}

data class ConcreteClass(
    override val type: ClassType,
    val superClass: ClassType?,
    val interfaces: List<ClassType>,
    val annotations: List<KvmAnnotation>,
    override val methods: List<PresentMethodDeclaration>,
    override val fields: List<ConcreteField>,
    val accessFlags: AccessFlags,
) : ClassDeclaration

val ConcreteClass.superTypes: List<ClassType> get() = listOfNotNull(superClass) + interfaces

data class PhantomClass(
    override val type: ClassType,
    override val methods: List<PhantomMethod> = emptyList(),
    override val fields: List<PhantomField> = emptyList(),
) : ClassDeclaration