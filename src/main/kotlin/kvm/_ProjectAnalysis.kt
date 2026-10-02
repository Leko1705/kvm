package com.leko.kvm

import com.leko.kvm.typing.ClassType

/**
 * A scope for analyzing a [Project], created through [analyze].
 *
 * It provides member extensions that resolve types against [project], so analysis
 * code can write `type.declaration` instead of passing the project around.
 *
 * @property project The project that lookups are resolved against.
 */
class ProjectAnalysis @PublishedApi internal constructor(val project: Project) {

    /**
     * The declaration of this class type within [project].
     *
     * If no class file in the project defines the type, a [PhantomClass] with no
     * known members is returned instead, so this never fails. The lookup is a
     * linear scan over all classes, and each access repeats it.
     */
    val ClassType.declaration: ClassDeclaration
        get() = project.classes.firstOrNull { this == it.type } ?: PhantomClass(this)

    /**
     * The full definition of this class type within [project], or `null` if the
     * project does not define it. This is a linear scan, repeated on each access.
     */
    val ClassType.concreteDeclaration: ConcreteClass?
        get() = project.concreteClasses.firstOrNull { this == it.type }

}

/**
 * Runs [block] with a [ProjectAnalysis] scope for [project] and returns its result.
 *
 * ```kotlin
 * val superName = analyze(project) {
 *     someType.concreteDeclaration?.superClass
 * }
 * ```
 *
 * @param project The project to analyze.
 * @param block Code to run with the analysis scope as its receiver.
 * @return The value returned by [block].
 */
inline fun <T> analyze(project: Project, block: ProjectAnalysis.() -> T): T =
    ProjectAnalysis(project).block()