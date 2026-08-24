package com.leko.kvm.cfg

import com.leko.kvm.bytecode.Instruction

sealed interface Block {
    val instructions: List<Instruction>
    val successors: List<Block>
}

sealed interface TerminationBlock : Block

class BasicBlock(
    override val instructions: List<Instruction>,
) : Block {
    lateinit var successor: Block
    override val successors: List<Block> get() = listOf(successor)
    override fun toString() = "BasicBlock(${instructions.size} instrs)"
}

class BranchBlock(
    val condition: List<Instruction>,
) : Block {
    lateinit var then: Block
    lateinit var `else`: Block
    override val instructions: List<Instruction> get() = condition
    override val successors: List<Block> get() = listOf(then, `else`)
    override fun toString() = "BranchBlock(${condition.size} instrs)"
}

class GotoBlock(
    override val instructions: List<Instruction>,
) : Block {
    lateinit var next: Block
    override val successors: List<Block> get() = listOf(next)
    override fun toString() = "GotoBlock(${instructions.size} instrs)"
}

class SwitchBlock(
    val dispatch: List<Instruction>,
) : Block {
    lateinit var cases: Map<Int, Block>
    lateinit var default: Block
    override val instructions: List<Instruction> get() = dispatch
    override val successors: List<Block> get() = cases.values.toList() + default
    override fun toString() = "SwitchBlock(${dispatch.size} instrs, ${cases.size} cases)"
}

class ReturnBlock(
    val value: List<Instruction>,
) : TerminationBlock {
    override val instructions: List<Instruction> get() = value
    override val successors: List<Block> = emptyList()
    override fun toString() = "ReturnBlock(${value.size} instrs)"
}

class ThrowBlock(
    val throwing: List<Instruction>,
) : TerminationBlock {
    override val instructions: List<Instruction> = throwing
    override val successors: List<Block> = emptyList()
    override fun toString() = "ThrowBlock(${throwing.size} instrs)"
}