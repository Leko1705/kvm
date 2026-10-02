package com.leko.kvm.callgraph

import com.leko.kvm.ClassDeclaration
import com.leko.kvm.ConcreteClass
import com.leko.kvm.MethodDeclaration
import com.leko.kvm.MethodFlags
import com.leko.kvm.name
import com.leko.kvm.parameterTypes
import com.leko.kvm.returnType
import com.leko.kvm.typing.ArrayType
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.VoidType
import kotlin.collections.asSequence
import kotlin.collections.orEmpty

/**
 * Entry-point selector for executable programs: the `main(String[])` methods
 * of the concrete classes in this list.
 *
 * ```kotlin
 * classes.callGraph(entryPoints = { mainMethods() })
 * ```
 */
fun List<ClassDeclaration>.mainMethods(): Set<MethodDeclaration> =
    this.flatMap { (it as? ConcreteClass)?.methods.orEmpty().asSequence() }
        .filter {
            it.name == "main"
                    && it.returnType == VoidType
                    && it.parameterTypes == listOf(ArrayType(ClassType("java.lang.String")))
                    && it.accessFlags.hasAll(MethodFlags.PUBLIC + MethodFlags.STATIC)
        }
        .toSet()

/**
 * Entry-point selector for libraries: every public method of the concrete classes
 * in this list, which is the default for call graph construction.
 *
 * Includes public constructors and abstract methods. The visibility of the declaring
 * class and protected methods are not considered.
 */
fun List<ClassDeclaration>.publicMethods(): Set<MethodDeclaration> =
    this.flatMap { (it as? ConcreteClass)?.methods.orEmpty().asSequence() }
        .filter { it.accessFlags.isPublic }
        .toSet()
