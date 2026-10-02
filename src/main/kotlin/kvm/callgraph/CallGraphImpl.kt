package com.leko.kvm.callgraph

import com.leko.kvm.MethodDeclaration

/**
 * Default [CallGraph] implementation backed by a flat edge list.
 *
 * The caller and callee indexes are built lazily, on the first call to
 * [calleesOf] or [callersOf], so constructing a graph is cheap. Lookups are
 * by method equality, not identity.
 *
 * @property entryPoints The methods the analysis started from.
 * @property edges Every call edge. The list is used as given and not copied.
 */
class CallGraphImpl(
    override val entryPoints: Set<MethodDeclaration>,
    override val edges: List<CallEdge>,
) : CallGraph {
    private val byCaller by lazy { edges.groupBy { it.site.caller } }
    private val byCallee by lazy { edges.groupBy { it.callee } }

    override fun calleesOf(method: MethodDeclaration) = byCaller[method].orEmpty()
    override fun callersOf(method: MethodDeclaration) = byCallee[method].orEmpty()
}