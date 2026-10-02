package com.leko.kvm.callgraph

import com.leko.kvm.MethodDeclaration

sealed interface CallGraphType {
    data class ClassHierarchyAnalysis(val entryPoints: Set<MethodDeclaration>) : CallGraphType
    data class RapidTypeAnalysis(val entryPoints: Set<MethodDeclaration>) : CallGraphType
}
