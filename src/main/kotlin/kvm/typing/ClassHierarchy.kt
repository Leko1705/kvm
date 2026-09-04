package com.leko.kvm.typing

import com.leko.kvm.*

class ClassHierarchy(classFiles: Iterable<ClassDeclaration>) {

    internal constructor(project: Project) : this(project.jars.flatMap { it.classFiles.asSequence() }.map { it.declaration })

    private val byType: Map<ClassType, ClassDeclaration> =
        classFiles.associateBy { it.type }

    private val directSubtypes: Map<ClassType, List<ClassType>> =
        buildMap<ClassType, MutableList<ClassType>> {
            for (decl in byType.values) {
                if (decl !is ConcreteClass) continue
                for (parent in listOfNotNull(decl.superClass) + decl.interfaces) {
                    getOrPut(parent) { mutableListOf() }.add(decl.type)
                }
            }
        }

    fun declarationOf(type: ClassType): ClassDeclaration = byType[type] ?: PhantomClass(type, emptyList(), emptyList())

    /** `type` plus every transitive subclass/implementer. */
    fun subtypes(type: ClassType, inclusive: Boolean = true): Set<ClassType> {
        val result = mutableSetOf(type)
        val worklist = ArrayDeque<ClassType>().apply { add(type) }
        while (worklist.isNotEmpty()) {
            for (sub in directSubtypes[worklist.removeFirst()].orEmpty()) {
                if (result.add(sub)) worklist.add(sub)
            }
        }
        if (!inclusive) result.remove(type)
        return result
    }

    /** Standard method-resolution walk: first class from `startType` upward that declares it. */
    fun resolveMethod(startType: ClassType, target: MethodSignature): MethodDeclaration {
        var current: ClassType? = startType
        while (current != null) {
            when (val decl = declarationOf(current)) {
                is ConcreteClass -> {
                    decl.methods.firstOrNull { it.signature == target }?.let { return it }
                    current = decl.superClass
                }
                is PhantomClass -> return PhantomMethod(decl.type, target)
            }
        }
        return PhantomMethod(startType, target)
    }
}
