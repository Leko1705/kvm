package com.leko.kvm.callgraph

import com.leko.kvm.ConcreteMethod

interface CallGraphSpecification {
    companion object Companion

    /** Should we scan this method's body? Checked before touching `body`. */
    fun shouldExpand(method: ConcreteMethod): Boolean

    /** Should we resolve targets for this site? Checked before resolution. */
    fun shouldResolve(site: CallSite): Boolean

    /** Should this resolved edge be recorded? Checked after resolution. */
    fun shouldInclude(edge: CallEdge): Boolean

}


abstract class AbstractCallGraphSpecification: CallGraphSpecification {

    override fun shouldExpand(method: ConcreteMethod): Boolean = true

    override fun shouldResolve(site: CallSite): Boolean = true

    override fun shouldInclude(edge: CallEdge): Boolean = true

}

data object NoCallGraphSpecification : AbstractCallGraphSpecification()

data object EmptyCallGraphSpecification : AbstractCallGraphSpecification() {
    override fun shouldExpand(method: ConcreteMethod): Boolean = false
    override fun shouldResolve(site: CallSite): Boolean = false
    override fun shouldInclude(edge: CallEdge): Boolean = false
}


fun CallGraphSpecification.Companion.shouldExpand(
    predicate: (ConcreteMethod) -> Boolean)
: CallGraphSpecification = object: AbstractCallGraphSpecification() {
    override fun shouldExpand(method: ConcreteMethod): Boolean = predicate(method)
}

fun CallGraphSpecification.Companion.shouldResolve(
    predicate: (CallSite) -> Boolean)
        : CallGraphSpecification = object: AbstractCallGraphSpecification() {
    override fun shouldResolve(site: CallSite): Boolean = predicate(site)
}

fun CallGraphSpecification.Companion.shouldInclude(
    predicate: (CallEdge) -> Boolean
): CallGraphSpecification = object: AbstractCallGraphSpecification() {
    override fun shouldInclude(edge: CallEdge): Boolean = predicate(edge)
}

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

infix fun CallGraphSpecification.and(other: CallGraphSpecification): CallGraphSpecification =
    when {
        this is NoCallGraphSpecification -> other
        other is NoCallGraphSpecification -> this
        this is EmptyCallGraphSpecification -> this
        other is EmptyCallGraphSpecification -> other
        this == other -> this
        else -> this.merge(other) { a, b -> a && b }
    }

infix fun CallGraphSpecification.or(other: CallGraphSpecification): CallGraphSpecification =
    when {
        this is NoCallGraphSpecification -> this
        other is NoCallGraphSpecification -> other
        this is EmptyCallGraphSpecification -> other
        other is EmptyCallGraphSpecification -> this
        this == other -> this
        else -> this.merge(other) { a, b -> a || b }
    }

infix fun CallGraphSpecification.xor(other: CallGraphSpecification): CallGraphSpecification =
    when {
        this is NoCallGraphSpecification -> !other
        other is NoCallGraphSpecification -> !this
        this is EmptyCallGraphSpecification -> other
        other is EmptyCallGraphSpecification -> this
        this == other -> EmptyCallGraphSpecification
        else -> this.merge(other) { a, b -> a xor b }
    }

operator fun CallGraphSpecification.not(): CallGraphSpecification =
    when (this) {
        is NoCallGraphSpecification -> EmptyCallGraphSpecification
        is EmptyCallGraphSpecification -> NoCallGraphSpecification
        else -> map { !it }
    }

operator fun CallGraphSpecification.plus(other: CallGraphSpecification): CallGraphSpecification =
    this or other

operator fun CallGraphSpecification.minus(other: CallGraphSpecification): CallGraphSpecification =
    if (this is NoCallGraphSpecification)
        !other
    else
        this and !other

inline fun CallGraph.filter(filter: (CallEdge) -> Boolean): CallGraph =
    CallGraphImpl(entryPoints, edges.filter(filter))

fun CallGraph.filter(spec: CallGraphSpecification): CallGraph = filter(spec::shouldInclude)
