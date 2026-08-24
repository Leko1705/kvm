package com.leko.kvm.cfg

val ControlFlowGraph.liveBlocks: Set<Block> get() {
    val visited = mutableSetOf<Block>()
    val worklist = ArrayDeque<Block>().apply { add(entry) }
    while (worklist.isNotEmpty()) {
        val block = worklist.removeFirst()
        if (visited.add(block)) worklist.addAll(block.successors)
    }
    return visited
}

val ControlFlowGraph.deadBlocks: Set<Block> get() {
    val liveBlocks = liveBlocks
    return all.filterNot { it in liveBlocks }.toSet()
}

fun ControlFlowGraph.withoutDeadBlocks(): ControlFlowGraph {
    val blocks = liveBlocks
    val terminals = blocks.filterIsInstance<TerminationBlock>()
    return CFGImpl(entry, terminals, blocks.toList())
}