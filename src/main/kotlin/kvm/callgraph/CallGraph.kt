package com.leko.kvm.callgraph

import com.leko.kvm.bytecode.InvocationInstruction
import com.leko.kvm.ConcreteMethod
import com.leko.kvm.MethodDeclaration

data class CallSite(
    val caller: ConcreteMethod,
    val instruction: InvocationInstruction,
)

data class CallEdge(
    val site: CallSite,
    val callee: MethodDeclaration,
)

interface CallGraph {
    val entryPoints: Set<MethodDeclaration>
    val edges: List<CallEdge>

    fun calleesOf(method: MethodDeclaration): List<CallEdge>
    fun callersOf(method: MethodDeclaration): List<CallEdge>
}