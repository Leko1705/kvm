package com.leko.kvm.callgraph

import com.leko.kvm.MethodSignature

sealed interface CallGraphType {
    data class ClassHierarchyAnalysis(val entryPoints: Set<MethodSignature>) : CallGraphType
    data class RapidTypeAnalysis(val entryPoints: Set<MethodSignature>) : CallGraphType
}
