package com.leko.kvm.typing

import com.leko.kvm.*

/**
 * An index over a set of class declarations that answers inheritance questions:
 * which classes are subtypes of a type, what a type's supertypes are, and which
 * method a call resolves to.
 *
 * Indexes are built lazily on first use. After that the hierarchy is read-only,
 * so it can be queried from several threads at once.
 *
 * Types with no declaration among [classDecls] are treated as phantom: they have
 * no known supertypes or subtypes beyond what other classes declare.
 *
 * Do not mutate [classDecls] after construction, because it is read lazily.
 * If several declarations share a type, the last one wins.
 *
 * @param classDecls The classes that make up the hierarchy.
 */
class ClassHierarchy(classDecls: Iterable<ClassDeclaration>) {

    /** Builds a hierarchy from every class in every jar of [project]. */
    internal constructor(project: Project) : this(project.jars.flatMap { it.classFiles.asSequence() }.map { it.declaration })

    private val byType: Map<ClassType, ClassDeclaration> by lazy {
        classDecls.associateBy { it.type }
    }

    /** Maps each type to the classes that directly extend or implement it. */
    private val directSubtypes: Map<ClassType, List<ClassType>> by lazy {
        buildMap<ClassType, MutableList<ClassType>> {
            for (decl in byType.values) {
                if (decl !is ConcreteClass) continue
                for (parent in listOfNotNull(decl.superClass) + decl.interfaces) {
                    getOrPut(parent) { mutableListOf() }.add(decl.type)
                }
            }
        }
    }

    /**
     * The declaration of [type], or an empty [PhantomClass] if no class in this
     * hierarchy defines it. Never fails.
     */
    fun declarationOf(type: ClassType): ClassDeclaration = byType[type] ?: PhantomClass(type, emptyList(), emptyList())

    /**
     * [type] and every transitive subclass or implementer, found breadth-first.
     *
     * The result is a new set on each call, in discovery order, and the walk
     * costs time proportional to the size of the subtree. [type] does not need
     * to be declared in this hierarchy.
     *
     * @param inclusive Whether to include [type] itself. Defaults to `true`.
     */
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

    /**
     * The **direct** supertypes of [type]: its superclass (if any) followed by its
     * interfaces. Supertypes of those are not included.
     *
     * Empty if [type] is not a [ConcreteClass] in this hierarchy.
     *
     * @param inclusive Whether to append [type] itself at the end. Defaults to
     * `false`, unlike [subtypes].
     */
    fun supertypes(type: ClassType, inclusive: Boolean = false): List<ClassType> {
        val superTypes = (declarationOf(type) as? ConcreteClass)?.superTypes ?: emptyList()
        return if (inclusive) superTypes + type else superTypes
    }

    /**
     * Finds the method that a lookup of [target] starting at [startType] finds,
     * by walking up the superclass chain and returning the first class that
     * declares a method with the same signature.
     *
     * The result is a [PresentMethodDeclaration] if a declaration is found. It is
     * a [PhantomMethod] if the walk reaches a class that isn't declared here
     * (owner: that class) or runs out of superclasses (owner: [startType]).
     *
     * Only superclasses are searched. Interfaces are not, so a method inherited
     * from an interface, including a default method, is not found.
     *
     * @param startType The class to start the lookup at, usually the static
     * receiver type of the call.
     * @param target The method's name and descriptor.
     */
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