package com.leko.kvm

import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.Type

/**
 * Identifies a field: the class that declares it, its name and its type.
 *
 * @property owner The class that declares the field.
 * @property name The field's name.
 * @property type The field's type.
 */
data class FieldSignature(
    val owner: ClassType,
    val name: String,
    val type: Type,
)

/** Renders this signature in JVM syntax, for example `java/lang/System.out:Ljava/io/PrintStream;`. */
fun FieldSignature.readableJvm(): String =
    "${owner.jvmName}.$name:${type.jvmName}"

/**
 * The declaration of a field, either a [ConcreteField] read from an analyzed
 * class or a [PhantomField] known only through references.
 */
sealed interface FieldDeclaration : Element {
    /** The field's owner, name and type. */
    val signature: FieldSignature

    /** The field's access flags. */
    val accessFlags: FieldFlags
}

/** The class that declares this field. */
val FieldDeclaration.owner: ClassType get() = signature.owner

/** The name of this field. */
val FieldDeclaration.name: String get() = signature.name

/** The type of this field. */
val FieldDeclaration.type: Type get() = signature.type

/**
 * A field whose declaration was read from an analyzed class.
 *
 * @property signature The field's owner, name and type.
 * @property accessFlags The field's access flags.
 * @property annotations The annotations applied to the field.
 */
data class ConcreteField(
    override val signature: FieldSignature,
    override val accessFlags: FieldFlags,
    val annotations: List<KvmAnnotation>
) : FieldDeclaration

/**
 * A field that is referenced but whose declaration is not available, typically
 * because its owner class is outside the analyzed [Project].
 *
 * Because the real declaration was never seen, [accessFlags] is a placeholder
 * rather than the field's actual flags.
 *
 * @property signature The field's owner, name and type.
 * @property accessFlags Placeholder flags for the unseen declaration.
 */
data class PhantomField(
    override val signature: FieldSignature,
    override val accessFlags: FieldFlags
) : FieldDeclaration