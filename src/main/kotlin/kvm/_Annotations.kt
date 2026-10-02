package com.leko.kvm

import com.leko.kvm.typing.ClassType

/** Concrete classes in this project that carry [annotation]. */
@PublishedApi
internal fun Project.annotatedClasses(annotation: ClassType): Sequence<ClassDeclaration> =
    this.concreteClasses
        .filter { cls ->
            cls.annotations.any { ann -> ann.name == annotation.javaName }
        }

/** Concrete fields in this project that carry [annotation]. */
@PublishedApi
internal fun Project.annotatedFields(annotation: ClassType): Sequence<FieldDeclaration> =
    this.concreteFields
        .filter { cls ->
            cls.annotations.any { ann -> ann.name == annotation.javaName }
        }

/** Present methods (concrete and abstract) in this project that carry [annotation]. */
@PublishedApi
internal fun Project.annotatedMethods(annotation: ClassType): Sequence<MethodDeclaration> =
    this.presentMethods
        .filter { mth ->
            mth.annotations.any { ann -> ann.name == annotation.javaName }
        }

/**
 * Finds the elements of kind [T] in this project that are annotated with [annotation].
 *
 * [T] selects both the kind of element to search (classes, fields or methods)
 * and how specific the result type is. For example:
 *
 * ```kotlin
 * val classes = project.annotated<ConcreteClass>(ClassType("com/example/Entity"))
 * val methods = project.annotated<PresentMethodDeclaration>(ClassType("com/example/Entity"))
 * ```
 *
 * Annotations are matched by comparing [KvmAnnotation.name] to the annotation
 * type's [javaName][ClassType.javaName]. Only elements whose annotations were
 * read from a jar can match, so the phantom types ([PhantomClass], [PhantomField],
 * [PhantomMethod]) always yield an empty sequence, and the general types
 * ([ClassDeclaration], [FieldDeclaration], [MethodDeclaration]) simply skip phantom elements.
 *
 * @param T The element type to search for. Must be one of [ClassDeclaration],
 * [ConcreteClass], [PhantomClass], [FieldDeclaration], [ConcreteField],
 * [PhantomField], [MethodDeclaration], [PresentMethodDeclaration],
 * [ConcreteMethod], [AbstractMethod] or [PhantomMethod].
 * @param annotation The annotation type to look for.
 * @return A lazy sequence of the matching elements.
 * @throws IllegalStateException if [T] is not one of the supported types
 * (for example [Element] itself).
 */
inline fun <reified T : Element> Project.annotated(annotation: ClassType): Sequence<T> = when (T::class) {
    ClassDeclaration::class -> annotatedClasses(annotation).filterIsInstance<T>()
    ConcreteClass::class -> annotatedClasses(annotation).filterIsInstance<T>()
    PhantomClass::class -> annotatedClasses(annotation).filterIsInstance<T>()
    FieldDeclaration::class -> annotatedFields(annotation).filterIsInstance<T>()
    ConcreteField::class -> annotatedFields(annotation).filterIsInstance<T>()
    PhantomField::class -> annotatedClasses(annotation).filterIsInstance<T>()
    MethodDeclaration::class -> annotatedMethods(annotation).filterIsInstance<T>()
    PresentMethodDeclaration::class -> annotatedMethods(annotation).filterIsInstance<T>()
    ConcreteMethod::class -> annotatedMethods(annotation).filterIsInstance<T>()
    AbstractMethod::class -> annotatedMethods(annotation).filterIsInstance<T>()
    PhantomMethod::class -> annotatedClasses(annotation).filterIsInstance<T>()
    else -> error("Unexpected type ${T::class}")
}

/**
 * Overload of [annotated] that takes the annotation as a name string, which is
 * wrapped in a [ClassType].
 *
 * @param annotation The annotation type's name.
 */
inline fun <reified T : Element> Project.annotated(annotation: String): Sequence<T> =
    annotated(ClassType(annotation))