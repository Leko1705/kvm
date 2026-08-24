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
    ClassDeclaration::class -> @Suppress("UNCHECKED_CAST") (annotatedClasses(annotation) as Sequence<T>)
    FieldDeclaration::class -> @Suppress("UNCHECKED_CAST") (annotatedFields(annotation) as Sequence<T>)
    MethodDeclaration::class -> @Suppress("UNCHECKED_CAST") (annotatedMethods(annotation) as Sequence<T>)
    else -> error("Unexpected type ${T::class}")
}

inline fun <reified T : Element> Project.annotated(annotation: String): Sequence<T> =
    annotated(ClassType(annotation))

