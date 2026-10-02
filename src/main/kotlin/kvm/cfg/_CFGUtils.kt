package com.leko.kvm.cfg


/**
 * The blocks reachable from [entry][ControlFlowGraph.entry] by following
 * successor edges, found by breadth-first search.
 *
 * The returned set iterates in discovery order, starting with the entry block.
 * Because exception handler edges are not modeled, blocks reachable only
 * through a handler are not included. The set is recomputed on each access.
 */
val ControlFlowGraph.liveBlocks: Set<Block> get() {
    val visited = mutableSetOf<Block>()
    val worklist = ArrayDeque<Block>().apply { add(entry) }
    while (worklist.isNotEmpty()) {
        val block = worklist.removeFirst()
        if (visited.add(block)) worklist.addAll(block.successors)
    }
    return visited
}

/**
 * The blocks in [all][ControlFlowGraph.all] that are not reachable from the
 * entry block, which are the blocks absent from [liveBlocks].
 *
 * Recomputed on each access.
 */
val ControlFlowGraph.deadBlocks: Set<Block> get() {
    val liveBlocks = liveBlocks
    return all.filterNot { it in liveBlocks }.toSet()
}

/**
 * Returns a new graph containing only the [live][liveBlocks] blocks.
 *
 * The blocks themselves are shared with the original graph, not copied. Their
 * successors are unchanged, which is safe because every successor of a live
 * block is live. The new graph's [all][ControlFlowGraph.all] is in breadth-first
 * order rather than program order, and its exits are the live termination blocks.
 */
fun ControlFlowGraph.withoutDeadBlocks(): ControlFlowGraph {
    val blocks = liveBlocks
    val terminals = blocks.filterIsInstance<TerminationBlock>()
    return CFGImpl(entry, terminals, blocks.toList())
}