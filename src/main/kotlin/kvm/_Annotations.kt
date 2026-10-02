package com.leko.kvm

import com.leko.kvm.typing.ClassType

@PublishedApi
internal fun Project.annotatedClasses(annotation: ClassType): Sequence<ClassDeclaration> =
    this.concreteClasses
        .filter { cls ->
            cls.annotations.any { ann -> ann.name == annotation.javaName }
        }

@PublishedApi
internal fun Project.annotatedFields(annotation: ClassType): Sequence<FieldDeclaration> =
    this.concreteFields
        .filter { cls ->
            cls.annotations.any { ann -> ann.name == annotation.javaName }
        }

@PublishedApi
internal fun Project.annotatedMethods(annotation: ClassType): Sequence<MethodDeclaration> =
    this.presentMethods
        .filter { mth ->
            mth.annotations.any { ann -> ann.name == annotation.javaName }
        }

inline fun <reified T : Element> Project.annotated(annotation: ClassType): Sequence<T> = when (T::class) {
    ClassDeclaration::class -> annotatedClasses(annotation).filterIsInstance<T>()
    ConcreteClass::class -> annotatedClasses(annotation).filterIsInstance<T>()
    PhantomClass::class -> emptySequence()
    FieldDeclaration::class -> annotatedFields(annotation).filterIsInstance<T>()
    ConcreteField::class -> annotatedFields(annotation).filterIsInstance<T>()
    PhantomField::class -> emptySequence()
    MethodDeclaration::class -> annotatedMethods(annotation).filterIsInstance<T>()
    PresentMethodDeclaration::class -> annotatedMethods(annotation).filterIsInstance<T>()
    ConcreteMethod::class -> annotatedMethods(annotation).filterIsInstance<T>()
    AbstractMethod::class -> annotatedMethods(annotation).filterIsInstance<T>()
    PhantomMethod::class -> emptySequence()
    else -> error("Unexpected type ${T::class}")
}

inline fun <reified T : Element> Project.annotated(annotation: String): Sequence<T> =
    annotated(ClassType(annotation))

