package com.leko.kvm

import com.leko.kvm.typing.ClassHierarchy


val Project.classFiles: List<ClassFile> get() = jars.flatMap { it.classFiles }

val Project.classes: Sequence<ClassDeclaration> get() = classFiles.asSequence().map { it.declaration }

val Project.concreteClasses: Sequence<ConcreteClass> get() = classes.filterIsInstance<ConcreteClass>()

val Project.phantomClasses: Sequence<PhantomClass> get() = classes.filterIsInstance<PhantomClass>()

val Project.methods: Sequence<MethodDeclaration> get() = classes.flatMap { it.methods }

val Project.presentMethods: Sequence<PresentMethodDeclaration> get() = concreteClasses.flatMap { it.methods }

val Project.phantomMethods: Sequence<PhantomMethod> get() = phantomClasses.flatMap { it.methods }

val Project.fields: Sequence<FieldDeclaration> get() = classes.flatMap { it.fields }

val Project.concreteFields: Sequence<ConcreteField> get() = concreteClasses.flatMap { it.fields }

val Project.phantomFields: Sequence<PhantomField> get() = phantomClasses.flatMap { it.fields }

fun Project.classHierarchy(): ClassHierarchy = ClassHierarchy(this)
