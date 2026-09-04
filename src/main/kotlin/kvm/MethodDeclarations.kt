package com.leko.kvm

import com.leko.kvm.bytecode.ExceptionHandler
import com.leko.kvm.bytecode.Instruction
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.Type


data class MethodDescriptor(
    val parameterTypes: List<Type>,
    val returnType: Type,
)

fun MethodDescriptor.readableJvm(): String =
    "${parameterTypes.joinToString("", "(", ")") { it.jvmName }}${returnType.jvmName}"

fun MethodDescriptor.readableKotlin(): String =
    "${parameterTypes.joinToString("", "(", ")") { it.jvmName }}: ${returnType.jvmName}"


data class MethodSignature(
    val name: String,
    val descriptor: MethodDescriptor
)

val MethodSignature.parameterTypes: List<Type> get() = descriptor.parameterTypes

val MethodSignature.returnType: Type get() = descriptor.returnType

fun MethodSignature.readableJvm(): String = "$name${descriptor.readableJvm()}"

fun MethodSignature.readableKotlin(): String = "$name${descriptor.readableKotlin()}"

fun MethodSignature.readableJava(): String =
    "${returnType.jvmName} $name${parameterTypes.joinToString("", "(", ")") { it.javaName }}"

sealed interface MethodDeclaration : Element {
    val owner: ClassType
    val signature: MethodSignature
}

sealed interface PresentMethodDeclaration : MethodDeclaration {
    val accessFlags: MethodFlags
    val annotations: List<KvmAnnotation>
}

class ConcreteMethod(
    override val owner: ClassType,
    override val signature: MethodSignature,
    override val accessFlags: MethodFlags,
    override val annotations: List<KvmAnnotation>,
    bodySupplier: () -> MethodBody,
) : PresentMethodDeclaration {

    constructor(
        owner: ClassType,
        signature: MethodSignature,
        accessFlags: MethodFlags,
        annotations: List<KvmAnnotation>,
        body: MethodBody
    ) : this(owner, signature, accessFlags, annotations, { body })

    val body: MethodBody by lazy(bodySupplier)

    override fun equals(other: Any?): Boolean =
        other is ConcreteMethod
                && signature == other.signature
                && this.accessFlags == other.accessFlags
                && this.annotations == other.annotations

    override fun hashCode(): Int = signature.hashCode()
    override fun toString(): String = signature.readableJvm()
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
    override val owner: ClassType,
    override val signature: MethodSignature,
    override val accessFlags: MethodFlags,
    override val annotations: List<KvmAnnotation>,
) : PresentMethodDeclaration {
    override fun toString(): String = signature.readableJvm()
}

data class PhantomMethod(
    override val owner: ClassType,
    override val signature: MethodSignature
) : MethodDeclaration {
    override fun toString(): String = signature.readableJvm()
}