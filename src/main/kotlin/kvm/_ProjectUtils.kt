package com.leko.kvm

import com.leko.kvm.typing.ClassHierarchy

/**
 * All class files across every jar in this project, in jar order.
 *
 * Unlike the other accessors below, this is an eagerly built [List]; it is
 * recomputed on every access.
 */
val Project.classFiles: List<ClassFile> get() = jars.flatMap { it.classFiles }

/**
 * The declarations of every class file in this project, as a lazy sequence.
 *
 * Contains both [ConcreteClass]es and [PhantomClass]es. Phantom classes only
 * appear here if a class file's declaration is itself phantom; classes that are
 * merely referenced from bytecode are not discovered by this property.
 */
val Project.classes: Sequence<ClassDeclaration> get() = classFiles.asSequence().map { it.declaration }

/** The subset of [classes] whose full definition is available. */
val Project.concreteClasses: Sequence<ConcreteClass> get() = classes.filterIsInstance<ConcreteClass>()

/** The subset of [classes] that are [PhantomClass]es. */
val Project.phantomClasses: Sequence<PhantomClass> get() = classes.filterIsInstance<PhantomClass>()

/** Every method declared by any class in this project, concrete or phantom. */
val Project.methods: Sequence<MethodDeclaration> get() = classes.flatMap { it.methods }

/** Methods declared by [concreteClasses]: both [ConcreteMethod]s and [AbstractMethod]s. */
val Project.presentMethods: Sequence<PresentMethodDeclaration> get() = concreteClasses.flatMap { it.methods }

/** Methods declared by [phantomClasses]. */
val Project.phantomMethods: Sequence<PhantomMethod> get() = phantomClasses.flatMap { it.methods }

/** Every field declared by any class in this project, concrete or phantom. */
val Project.fields: Sequence<FieldDeclaration> get() = classes.flatMap { it.fields }

/** Fields declared by [concreteClasses]. */
val Project.concreteFields: Sequence<ConcreteField> get() = concreteClasses.flatMap { it.fields }

/** Fields declared by [phantomClasses]. */
val Project.phantomFields: Sequence<PhantomField> get() = phantomClasses.flatMap { it.fields }

/**
 * Builds a [ClassHierarchy] over the classes of this project, for querying
 * subtype and supertype relationships.
 */
fun Project.classHierarchy(): ClassHierarchy = ClassHierarchy(this)