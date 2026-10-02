package com.leko.kvm.callgraph

import com.leko.kvm.ConcreteMethod

interface CallGraphSpecification {
    companion object Companion

    /** Should we scan this method's body? Checked before touching `body`. */
    suspend fun shouldExpand(method: ConcreteMethod): Boolean

    /** Should we resolve targets for this site? Checked before resolution. */
    suspend fun shouldResolve(site: CallSite): Boolean

    /** Should this resolved edge be recorded? Checked after resolution. */
    suspend fun shouldInclude(edge: CallEdge): Boolean

}


abstract class AbstractCallGraphSpecification: CallGraphSpecification {

    override suspend fun shouldExpand(method: ConcreteMethod): Boolean = true

    override suspend fun shouldResolve(site: CallSite): Boolean = true

    override suspend fun shouldInclude(edge: CallEdge): Boolean = true

}

data object NoCallGraphSpecification : AbstractCallGraphSpecification()

data object EmptyCallGraphSpecification : AbstractCallGraphSpecification() {
    override suspend fun shouldExpand(method: ConcreteMethod): Boolean = false
    override suspend fun shouldResolve(site: CallSite): Boolean = false
    override suspend fun shouldInclude(edge: CallEdge): Boolean = false
}


fun CallGraphSpecification.Companion.shouldExpand(
    predicate: suspend (ConcreteMethod) -> Boolean)
: CallGraphSpecification = object: AbstractCallGraphSpecification() {
    override suspend fun shouldExpand(method: ConcreteMethod): Boolean = predicate(method)
}

fun CallGraphSpecification.Companion.shouldResolve(
    predicate: suspend (CallSite) -> Boolean)
        : CallGraphSpecification = object: AbstractCallGraphSpecification() {
    override suspend fun shouldResolve(site: CallSite): Boolean = predicate(site)
}

fun CallGraphSpecification.Companion.shouldInclude(
    predicate: suspend (CallEdge) -> Boolean
): CallGraphSpecification = object: AbstractCallGraphSpecification() {
    override suspend fun shouldInclude(edge: CallEdge): Boolean = predicate(edge)
}

private fun CallGraphSpecification.map(
    map: suspend (Boolean) -> Boolean
): CallGraphSpecification = object : CallGraphSpecification {
    override suspend fun shouldExpand(method: ConcreteMethod): Boolean =
        map(this@map.shouldExpand(method))
    override suspend fun shouldResolve(site: CallSite): Boolean =
        map(this@map.shouldResolve(site))
    override suspend fun shouldInclude(edge: CallEdge): Boolean =
        map(this@map.shouldInclude(edge))
}

private fun CallGraphSpecification.merge(
    other: CallGraphSpecification,
    merge: suspend (Boolean, Boolean) -> Boolean
): CallGraphSpecification = object : CallGraphSpecification {
    override suspend fun shouldExpand(method: ConcreteMethod): Boolean =
        merge(this@merge.shouldExpand(method), other.shouldExpand(method))
    override suspend fun shouldResolve(site: CallSite): Boolean =
        merge(this@merge.shouldResolve(site), other.shouldResolve(site))
    override suspend fun shouldInclude(edge: CallEdge): Boolean =
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
