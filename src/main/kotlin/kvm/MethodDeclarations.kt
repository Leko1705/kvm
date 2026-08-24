package com.leko.kvm

import com.leko.kvm.bytecode.ExceptionHandler
import com.leko.kvm.bytecode.Instruction
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.Type

data class MethodSignature(
    val owner: ClassType,
    val name: String,
    val parameterTypes: List<Type>,
    val returnType: Type,
)

fun MethodSignature.readable(): String =
    "${owner.jvmName}.$name:${parameterTypes.joinToString("", "(", ")") { it.jvmName }}${returnType.jvmName}"

sealed interface MethodDeclaration : Element {
    val signature: MethodSignature
}

sealed interface PresentMethodDeclaration : MethodDeclaration {
    val accessFlags: MethodFlags
    val annotations: List<KvmAnnotation>
}

class ConcreteMethod(
    override val signature: MethodSignature,
    override val accessFlags: MethodFlags,
    override val annotations: List<KvmAnnotation>,
    bodySupplier: () -> MethodBody,
) : PresentMethodDeclaration {

    constructor(
        signature: MethodSignature,
        accessFlags: MethodFlags,
        annotations: List<KvmAnnotation>,
        body: MethodBody
    ) : this(signature, accessFlags, annotations, { body })

    val body: MethodBody by lazy(bodySupplier)

    override fun equals(other: Any?): Boolean =
        other is ConcreteMethod
                && signature == other.signature
                && this.accessFlags == other.accessFlags
                && this.annotations == other.annotations

    override fun hashCode(): Int = signature.hashCode()
    override fun toString(): String = signature.readable()
}

class MethodBody(
    instructionsSupplier: () -> List<Instruction>,
    handlersSupplier: () -> List<ExceptionHandler>,
) {
    constructor(instructions: List<Instruction>, handlers: List<ExceptionHandler>):
            this({ instructions }, { handlers })

    val instructions: List<Instruction> by lazy(instructionsSupplier)
    val handlers: List<ExceptionHandler> by lazy(handlersSupplier)
}

data class AbstractMethod(
    override val signature: MethodSignature,
    override val accessFlags: MethodFlags,
    override val annotations: List<KvmAnnotation>,
) : PresentMethodDeclaration {
    override fun toString(): String = signature.readable()
}

data class PhantomMethod(
    override val signature: MethodSignature
) : MethodDeclaration {
    override fun toString(): String = signature.readable()
}