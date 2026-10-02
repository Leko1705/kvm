package com.leko.kvm.callgraph

import com.leko.kvm.bytecode.InvocationInstruction
import com.leko.kvm.ConcreteMethod
import com.leko.kvm.MethodDeclaration

/**
 * A single invocation instruction inside a method body: the place where a call happens.
 *
 * A site is not tied to a particular callee. A virtual call site can have several
 * [CallEdge]s, one per possible target.
 *
 * Equality is structural, so two sites in the same method are only distinct if
 * their [instruction]s are not equal.
 *
 * @property caller The method whose body contains the invocation.
 * @property instruction The invoke instruction itself.
 */
data class CallSite(
    val caller: ConcreteMethod,
    val instruction: InvocationInstruction,
)

/**
 * A possible call from a [CallSite] to a [callee].
 *
 * The callee may be any [MethodDeclaration]. A [com.leko.kvm.PhantomMethod] means the
 * target is outside the analyzed classes, and an abstract method means no implementation
 * could be resolved.
 *
 * @property site The call site.
 * @property callee The method that may be invoked from [site].
 */
data class CallEdge(
    val site: CallSite,
    val callee: MethodDeclaration,
)

/**
 * A call graph: the set of calls that may happen between methods, starting from
 * a set of entry points.
 *
 * Which edges are present depends on the [CallGraphType] that built the graph
 * (for example [CHA] or [RTA]) and on the [CallGraphSpecification] it was given.
 */
interface CallGraph {
    /** The methods the analysis started from. */
    val entryPoints: Set<MethodDeclaration>

    /** Every call edge in the graph. */
    val edges: List<CallEdge>

    /**
     * The edges leaving [method], meaning all calls made by its body.
     *
     * Empty if [method] is not a [ConcreteMethod], was never expanded, or makes no calls.
     */
    fun calleesOf(method: MethodDeclaration): List<CallEdge>

    /**
     * The edges that arrive at [method], meaning every call site that may invoke it.
     *
     * Empty if nothing in the graph calls [method].
     */
    fun callersOf(method: MethodDeclaration): List<CallEdge>
}