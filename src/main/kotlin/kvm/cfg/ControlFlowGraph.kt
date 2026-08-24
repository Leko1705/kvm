package com.leko.kvm.cfg

interface ControlFlowGraph {

    val entry: Block

    val exists: List<TerminationBlock>

    val all: List<Block>

}