package com.leko.kvm.cfg

/**
 * Default [ControlFlowGraph] implementation, which just holds the given blocks.
 *
 * @property entry The entry block.
 * @property exits The exit blocks.
 * @property all All blocks of the graph.
 */
class CFGImpl(
    override val entry: Block,
    override val exits: List<TerminationBlock>,
    override val all: List<Block>
) : ControlFlowGraph