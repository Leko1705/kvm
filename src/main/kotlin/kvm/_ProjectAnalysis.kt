package com.leko.kvm

import com.leko.kvm.typing.ClassType

class ProjectAnalysis @PublishedApi internal constructor(val project: Project) {

    val ClassType.declaration: ClassDeclaration
        get() = project.classes.firstOrNull { this == it.type } ?: PhantomClass(this)

    val ClassType.concreteDeclaration: ConcreteClass?
        get() = project.concreteClasses.firstOrNull { this == it.type }

    val MethodSignature.declaration: MethodDeclaration
        get() = owner.declaration.methods.firstOrNull { it.signature == this } ?: PhantomMethod(this)

    val MethodSignature.presentDeclaration: PresentMethodDeclaration?
        get() = project.presentMethods.firstOrNull { it.signature == this }

}

inline fun <T> analyze(project: Project, block: ProjectAnalysis.() -> T): T =
    ProjectAnalysis(project).block()
