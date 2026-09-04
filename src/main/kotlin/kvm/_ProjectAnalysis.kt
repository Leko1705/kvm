package com.leko.kvm

import com.leko.kvm.typing.ClassType

class ProjectAnalysis @PublishedApi internal constructor(val project: Project) {

    val ClassType.declaration: ClassDeclaration
        get() = project.classes.firstOrNull { this == it.type } ?: PhantomClass(this)

    val ClassType.concreteDeclaration: ConcreteClass?
        get() = project.concreteClasses.firstOrNull { this == it.type }

}

inline fun <T> analyze(project: Project, block: ProjectAnalysis.() -> T): T =
    ProjectAnalysis(project).block()
