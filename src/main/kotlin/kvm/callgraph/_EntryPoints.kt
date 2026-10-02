package com.leko.kvm.callgraph

import com.leko.kvm.ClassDeclaration
import com.leko.kvm.ConcreteClass
import com.leko.kvm.MethodDeclaration
import com.leko.kvm.name
import com.leko.kvm.parameterTypes
import com.leko.kvm.returnType
import com.leko.kvm.typing.ArrayType
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.VoidType
import kotlin.collections.asSequence
import kotlin.collections.orEmpty

fun List<ClassDeclaration>.mainMethods(): Set<MethodDeclaration> =
    this.flatMap { (it as? ConcreteClass)?.methods.orEmpty().asSequence() }
        .filter {
            it.name == "main"
                    && it.returnType == VoidType
                    && it.parameterTypes == listOf(ArrayType(ClassType("java.lang.String")))
        }
        .toSet()

fun List<ClassDeclaration>.publicMethods(): Set<MethodDeclaration> =
    this.flatMap { (it as? ConcreteClass)?.methods.orEmpty().asSequence() }
        .filter { it.accessFlags.isPublic }
        .toSet()
