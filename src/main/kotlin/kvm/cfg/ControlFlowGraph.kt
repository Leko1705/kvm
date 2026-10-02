package com.leko.kvm.cfg

/**
 * The control flow graph of a single method, made of [Block]s connected by
 * their [successors][Block.successors].
 */
interface ControlFlowGraph {

    /** The block where execution starts. */
    val entry: Block

    /**
     * The blocks that end the method ([ReturnBlock]s and [ThrowBlock]s).
     */
    val exits: List<TerminationBlock>

    /** Every block in the graph, including any that are unreachable from [entry]. */
    val all: List<Block>

}