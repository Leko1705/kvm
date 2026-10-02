package com.leko.kvm

import com.leko.kvm.bytecode.ExceptionHandler
import com.leko.kvm.bytecode.Instruction
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.Type

/**
 * The parameter and return types of a method, corresponding to a JVM method
 * descriptor such as `(ILjava/lang/String;)V`.
 *
 * @property parameterTypes The parameter types, in declaration order.
 * @property returnType The return type.
 */
data class MethodDescriptor(
    val parameterTypes: List<Type>,
    val returnType: Type,
)

/**
 * Renders this descriptor in JVM descriptor syntax, for example `(ILjava/lang/String;)V`.
 */
fun MethodDescriptor.readableJvm(): String =
    "${parameterTypes.joinToString("", "(", ")") { it.jvmName }}${returnType.jvmName}"

/**
 * Renders this descriptor in a Kotlin-like form, with the return type after a
 * colon, for example `(ILjava/lang/String;): V`. Types are still shown by JVM name.
 */
fun MethodDescriptor.readableKotlin(): String =
    "${parameterTypes.joinToString("", "(", ")") { it.jvmName }}: ${returnType.jvmName}"

/**
 * The name and [descriptor][MethodDescriptor] of a method. Together these identify
 * a method within its owner class.
 *
 * @property name The method name (`<init>` for constructors, `<clinit>` for static initializers).
 * @property descriptor The method's parameter and return types.
 */
data class MethodSignature(
    val name: String,
    val descriptor: MethodDescriptor
)

/** The parameter types of this signature's descriptor. */
val MethodSignature.parameterTypes: List<Type> get() = descriptor.parameterTypes

/** The return type of this signature's descriptor. */
val MethodSignature.returnType: Type get() = descriptor.returnType

/** Renders this signature in JVM syntax, for example `println(Ljava/lang/String;)V`. */
fun MethodSignature.readableJvm(): String = "$name${descriptor.readableJvm()}"

/** Renders this signature in a Kotlin-like form, for example `println(Ljava/lang/String;): V`. */
fun MethodSignature.readableKotlin(): String = "$name${descriptor.readableKotlin()}"

/**
 * Renders this signature in a Java-like form: the return type, then the name,
 * then the parameters.
 */
fun MethodSignature.readableJava(): String =
    "${returnType.jvmName} $name${parameterTypes.joinToString(", ", "(", ")") { it.javaName }}"

/**
 * The declaration of a method.
 *
 * The method is either [present][PresentMethodDeclaration] in an analyzed class
 * or a [PhantomMethod] known only through references.
 */
sealed interface MethodDeclaration : Element {
    /** The class that declares this method. */
    val owner: ClassType

    /** The method's name and descriptor. */
    val signature: MethodSignature
}

/** The name of this method. */
val MethodDeclaration.name: String get() = signature.name

/** The parameter types of this method. */
val MethodDeclaration.parameterTypes: List<Type> get() = signature.parameterTypes

/** The return type of this method. */
val MethodDeclaration.returnType: Type get() = signature.returnType

/**
 * A method whose declaration was read from an analyzed class, so its flags and
 * annotations are known. Either a [ConcreteMethod] (has a body) or an
 * [AbstractMethod] (has none).
 */
sealed interface PresentMethodDeclaration : MethodDeclaration {
    /** The method's access flags. */
    val accessFlags: MethodFlags

    /** The annotations applied to the method. */
    val annotations: List<KvmAnnotation>
}

/**
 * A method with an implementation.
 *
 * The [body] is computed lazily the first time it is accessed, so loading a
 * class does not require decoding the bytecode of every method.
 *
 * Equality is based on [owner], [signature], [accessFlags] and [annotations]
 * only. The body is deliberately excluded.
 *
 * @property owner The class that declares this method.
 * @property signature The method's name and descriptor.
 * @property accessFlags The method's access flags.
 * @property annotations The annotations applied to the method.
 * @param bodySupplier Computes the body on first access. It is invoked at most once.
 */
class ConcreteMethod(
    override val owner: ClassType,
    override val signature: MethodSignature,
    override val accessFlags: MethodFlags,
    override val annotations: List<KvmAnnotation>,
    bodySupplier: () -> MethodBody,
) : PresentMethodDeclaration {

    /**
     * Creates a method with an already-available [body].
     */
    constructor(
        owner: ClassType,
        signature: MethodSignature,
        accessFlags: MethodFlags,
        annotations: List<KvmAnnotation>,
        body: MethodBody
    ) : this(owner, signature, accessFlags, annotations, { body })

    /** The method's code, computed lazily on first access. */
    val body: MethodBody by lazy(bodySupplier)

    override fun equals(other: Any?): Boolean =
        other is ConcreteMethod
                && this.owner == other.owner
                && signature == other.signature
                && this.accessFlags == other.accessFlags
                && this.annotations == other.annotations

    override fun hashCode(): Int = signature.hashCode()
    override fun toString(): String = signature.readableJvm()
}

/**
 * The code of a [ConcreteMethod]: its instructions and exception handlers.
 *
 * Both parts are computed lazily and independently, so reading only one of them
 * does not pay for the other.
 *
 * @param instructionsSupplier Computes the instruction list on first access.
 * @param handlersSupplier Computes the exception handler table on first access.
 */
class MethodBody(
    instructionsSupplier: () -> List<Instruction>,
    handlersSupplier: () -> List<ExceptionHandler>,
) {
    /**
     * Creates a body from already-available [instructions] and [handlers].
     */
    constructor(instructions: List<Instruction>, handlers: List<ExceptionHandler>):
            this({ instructions }, { handlers })

    /** The method's instructions, in bytecode order. */
    val instructions: List<Instruction> by lazy(instructionsSupplier)

    /** The method's exception handler table. */
    val handlers: List<ExceptionHandler> by lazy(handlersSupplier)
}

/**
 * A method without an implementation, such as an abstract or interface method.
 *
 * @property owner The class that declares this method.
 * @property signature The method's name and descriptor.
 * @property accessFlags The method's access flags.
 * @property annotations The annotations applied to the method.
 */
data class AbstractMethod(
    override val owner: ClassType,
    override val signature: MethodSignature,
    override val accessFlags: MethodFlags,
    override val annotations: List<KvmAnnotation>,
) : PresentMethodDeclaration {
    override fun toString(): String = signature.readableJvm()
}

/**
 * A method that is referenced but whose declaration is not available, typically
 * because its owner class is outside the analyzed [Project]. Its flags and
 * annotations are unknown.
 *
 * @property owner The class the method was referenced on.
 * @property signature The method's name and descriptor.
 */
data class PhantomMethod(
    override val owner: ClassType,
    override val signature: MethodSignature
) : MethodDeclaration {
    override fun toString(): String = signature.readableJvm()
}