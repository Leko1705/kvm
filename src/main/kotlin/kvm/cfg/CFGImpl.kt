package com.leko.kvm.cfg

class CFGImpl(
    override val entry: Block,
    override val exists: List<TerminationBlock>,
    override val all: List<Block>
) : ControlFlowGraph