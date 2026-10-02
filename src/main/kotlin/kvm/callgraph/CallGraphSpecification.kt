package com.leko.kvm.callgraph

import com.leko.kvm.ConcreteMethod

/**
 * Policy that tells a call graph construction what to explore and what to keep.
 *
 * A [CallGraphType] consults the specification at three stages, from cheapest to
 * most expensive:
 *
 * 1. [shouldExpand]: before a method's body is read.
 * 2. [shouldResolve]: before the targets of a call site are computed.
 * 3. [shouldInclude]: after resolution, once per candidate edge.
 *
 * Returning `false` early avoids the cost of the later stages.
 *
 * Specifications can be created with the [Companion] factories ([shouldExpand],
 * [shouldResolve], [shouldInclude]) and combined with [and], [or], [xor], [not],
 * [plus] and [minus]. [NoCallGraphSpecification] allows everything and
 * [EmptyCallGraphSpecification] allows nothing.
 *
 * **Thread safety.** Construction scans methods in parallel, so [shouldResolve]
 * (and, for [CHA], [shouldInclude]) may be called concurrently from several
 * threads. Implementations must be thread-safe.
 */
interface CallGraphSpecification {
    /** Anchor for the factory extension functions, such as `CallGraphSpecification.shouldInclude { ... }`. */
    companion object Companion

    /**
     * Whether to scan the body of [method] for calls. Checked before its `body` is touched.
     *
     * Returning `false` does not remove edges that point *to* [method]. It only
     * means [method] contributes no outgoing edges.
     */
    fun shouldExpand(method: ConcreteMethod): Boolean

    /** Whether to resolve the possible targets of [site]. Checked before resolution. */
    fun shouldResolve(site: CallSite): Boolean

    /** Whether to record the resolved [edge]. Checked after resolution. */
    fun shouldInclude(edge: CallEdge): Boolean

}

/**
 * Base class for specifications that only customize some of the hooks.
 * Every hook returns `true` unless overridden.
 */
abstract class AbstractCallGraphSpecification: CallGraphSpecification {

    override fun shouldExpand(method: ConcreteMethod): Boolean = true

    override fun shouldResolve(site: CallSite): Boolean = true

    override fun shouldInclude(edge: CallEdge): Boolean = true

}

/**
 * The unrestricted specification: every hook returns `true`. It is the default
 * for call graph construction.
 *
 * In the boolean algebra of specifications it is the identity of [and] and the
 * absorbing element of [or].
 */
data object NoCallGraphSpecification : AbstractCallGraphSpecification()

/**
 * The empty specification: every hook returns `false`, so nothing is explored and
 * no edges are recorded. Construction short-circuits and returns a graph that has
 * entry points but no edges.
 *
 * In the boolean algebra of specifications it is the identity of [or] and the
 * absorbing element of [and].
 */
data object EmptyCallGraphSpecification : AbstractCallGraphSpecification() {
    override fun shouldExpand(method: ConcreteMethod): Boolean = false
    override fun shouldResolve(site: CallSite): Boolean = false
    override fun shouldInclude(edge: CallEdge): Boolean = false
}


/**
 * Creates a specification that restricts which methods are expanded using [predicate].
 * The other two hooks allow everything.
 *
 * ```kotlin
 * val spec = CallGraphSpecification.shouldExpand { it.owner.javaName.startsWith("com.example.") }
 * ```
 */
fun CallGraphSpecification.Companion.shouldExpand(
    predicate: (ConcreteMethod) -> Boolean)
        : CallGraphSpecification = object: AbstractCallGraphSpecification() {
    override fun shouldExpand(method: ConcreteMethod): Boolean = predicate(method)
}

/**
 * Creates a specification that restricts which call sites are resolved using [predicate].
 * The other two hooks allow everything.
 */
fun CallGraphSpecification.Companion.shouldResolve(
    predicate: (CallSite) -> Boolean)
        : CallGraphSpecification = object: AbstractCallGraphSpecification() {
    override fun shouldResolve(site: CallSite): Boolean = predicate(site)
}

/**
 * Creates a specification that restricts which edges are recorded using [predicate].
 * The other two hooks allow everything.
 */
fun CallGraphSpecification.Companion.shouldInclude(
    predicate: (CallEdge) -> Boolean
): CallGraphSpecification = object: AbstractCallGraphSpecification() {
    override fun shouldInclude(edge: CallEdge): Boolean = predicate(edge)
}

/** Applies [map] to the result of each hook of this specification. */
private fun CallGraphSpecification.map(
    map: (Boolean) -> Boolean
): CallGraphSpecification = object : CallGraphSpecification {
    override fun shouldExpand(method: ConcreteMethod): Boolean =
        map(this@map.shouldExpand(method))
    override fun shouldResolve(site: CallSite): Boolean =
        map(this@map.shouldResolve(site))
    override fun shouldInclude(edge: CallEdge): Boolean =
        map(this@map.shouldInclude(edge))
}

/**
 * Combines this specification with [other] hook by hook, using [merge] on the
 * two results. Both specifications are always evaluated (no short-circuiting).
 */
private fun CallGraphSpecification.merge(
    other: CallGraphSpecification,
    merge: (Boolean, Boolean) -> Boolean
): CallGraphSpecification = object : CallGraphSpecification {
    override fun shouldExpand(method: ConcreteMethod): Boolean =
        merge(this@merge.shouldExpand(method), other.shouldExpand(method))
    override fun shouldResolve(site: CallSite): Boolean =
        merge(this@merge.shouldResolve(site), other.shouldResolve(site))
    override fun shouldInclude(edge: CallEdge): Boolean =
        merge(this@merge.shouldInclude(edge), other.shouldInclude(edge))
}

/**
 * Intersection: each hook returns `true` only if the hooks of both specifications do.
 *
 * Simplifies when either side is [NoCallGraphSpecification] (returns the other),
 * [EmptyCallGraphSpecification] (returns the empty specification) or when both are equal.
 */
infix fun CallGraphSpecification.and(other: CallGraphSpecification): CallGraphSpecification =
    when {
        this is NoCallGraphSpecification -> other
        other is NoCallGraphSpecification -> this
        this is EmptyCallGraphSpecification -> this
        other is EmptyCallGraphSpecification -> other
        this == other -> this
        else -> this.merge(other) { a, b -> a && b }
    }

/**
 * Union: each hook returns `true` if the hook of either specification does.
 *
 * Simplifies when either side is [NoCallGraphSpecification] (returns the unrestricted
 * specification), [EmptyCallGraphSpecification] (returns the other) or when both are equal.
 */
infix fun CallGraphSpecification.or(other: CallGraphSpecification): CallGraphSpecification =
    when {
        this is NoCallGraphSpecification -> this
        other is NoCallGraphSpecification -> other
        this is EmptyCallGraphSpecification -> other
        other is EmptyCallGraphSpecification -> this
        this == other -> this
        else -> this.merge(other) { a, b -> a or b }
    }

/**
 * Exclusive or: each hook returns `true` if exactly one of the two hooks does.
 *
 * Equal operands cancel out to [EmptyCallGraphSpecification].
 */
infix fun CallGraphSpecification.xor(other: CallGraphSpecification): CallGraphSpecification =
    when {
        this is NoCallGraphSpecification -> !other
        other is NoCallGraphSpecification -> !this
        this is EmptyCallGraphSpecification -> other
        other is EmptyCallGraphSpecification -> this
        this == other -> EmptyCallGraphSpecification
        else -> this.merge(other) { a, b -> a xor b }
    }

/**
 * Complement: inverts the result of every hook.
 *
 * `!NoCallGraphSpecification` is [EmptyCallGraphSpecification] and vice versa.
 *
 * Be careful with specifications made by the [Companion] factories: they answer
 * `true` for the hooks they don't customize, so negating them makes those hooks
 * answer `false`. For example, `!CallGraphSpecification.shouldInclude { ... }`
 * also stops every method from being expanded.
 */
operator fun CallGraphSpecification.not(): CallGraphSpecification =
    when (this) {
        is NoCallGraphSpecification -> EmptyCallGraphSpecification
        is EmptyCallGraphSpecification -> NoCallGraphSpecification
        else -> map { !it }
    }

/** Union of two specifications. Same as [or]. */
operator fun CallGraphSpecification.plus(other: CallGraphSpecification): CallGraphSpecification =
    this or other

/**
 * Difference: whatever this specification allows and [other] does not. Same as
 * `this and !other`, with the same caveat as [not] for partial specifications.
 */
operator fun CallGraphSpecification.minus(other: CallGraphSpecification): CallGraphSpecification =
    if (this is NoCallGraphSpecification)
        !other
    else
        this and !other

/**
 * Returns a new graph with the same entry points and only the edges that satisfy [filter].
 *
 * Nothing is pruned beyond that. Methods that become unreachable keep their edges,
 * and entry points are kept even if they end up with no edges.
 */
inline fun CallGraph.filter(filter: (CallEdge) -> Boolean): CallGraph =
    CallGraphImpl(entryPoints, edges.filter(filter))

/**
 * Returns a new graph keeping only the edges accepted by [spec]'s
 * [shouldInclude][CallGraphSpecification.shouldInclude]. The other two hooks
 * are ignored, since the graph is already built.
 */
fun CallGraph.filter(spec: CallGraphSpecification): CallGraph = filter(spec::shouldInclude)