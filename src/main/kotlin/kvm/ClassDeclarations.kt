package com.leko.kvm

import com.leko.kvm.typing.ClassType

/**
 * The declaration of a class, interface, enum or annotation.
 *
 * A declaration is either a [ConcreteClass], whose full definition was read from
 * a jar, or a [PhantomClass], which is only known through references to it.
 */
sealed interface ClassDeclaration : Element {
    /** The type this declaration defines. */
    val type: ClassType

    /** The methods declared by this class. */
    val methods: List<MethodDeclaration>

    /** The fields declared by this class. */
    val fields: List<FieldDeclaration>
}

/**
 * A class whose definition is available in an analyzed jar.
 *
 * @property type The type this class defines.
 * @property superClass The direct superclass, or `null` if there is none
 * (as for `java.lang.Object` and for interfaces in some representations).
 * @property interfaces The directly implemented (or extended) interfaces,
 * in declaration order.
 * @property annotations The annotations applied to the class.
 * @property methods The methods declared by the class, including abstract ones.
 * @property fields The fields declared by the class.
 * @property accessFlags The class's access flags.
 */
data class ConcreteClass(
    override val type: ClassType,
    val superClass: ClassType?,
    val interfaces: List<ClassType>,
    val annotations: List<KvmAnnotation>,
    override val methods: List<PresentMethodDeclaration>,
    override val fields: List<ConcreteField>,
    val accessFlags: ClassFlags,
) : ClassDeclaration

/**
 * All direct supertypes of this class: the [superclass][ConcreteClass.superClass]
 * (if any) followed by its [interfaces][ConcreteClass.interfaces].
 */
val ConcreteClass.superTypes: List<ClassType> get() = listOfNotNull(superClass) + interfaces

/**
 * A class that is referenced but whose definition is not available, for example
 * because it lives in a jar outside the analyzed [Project].
 *
 * It carries only the members that are known to exist because something
 * referenced them. Its flags, supertypes and annotations are unknown.
 *
 * @property type The referenced type.
 * @property methods The methods known to exist on this class.
 * @property fields The fields known to exist on this class.
 */
data class PhantomClass(
    override val type: ClassType,
    override val methods: List<PhantomMethod> = emptyList(),
    override val fields: List<PhantomField> = emptyList(),
) : ClassDeclaration