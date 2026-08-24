package com.leko.kvm.callgraph

import com.leko.kvm.MethodDeclaration

class CallGraphImpl(
    override val entryPoints: Set<MethodDeclaration>,
    override val edges: List<CallEdge>,
) : CallGraph {
    private val byCaller by lazy { edges.groupBy { it.site.caller } }
    private val byCallee by lazy { edges.groupBy { it.callee } }

    override fun calleesOf(method: MethodDeclaration) = byCaller[method].orEmpty()
    override fun callersOf(method: MethodDeclaration) = byCallee[method].orEmpty()
}