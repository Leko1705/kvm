package com.leko.kvm.cfg

import com.leko.kvm.bytecode.Instruction

/**
 * A basic block: a straight-line run of instructions with a single entry point
 * at the start and a single exit at the end.
 *
 * The kind of block is determined by how its last instruction transfers control.
 * The hierarchy is sealed, so `when` expressions over blocks can be exhaustive.
 *
 * Blocks use identity equality, so they can be used directly in sets and as map keys.
 */
sealed interface Block {
    /**
     * The instructions of this block, in order. A block that begins at a jump
     * target starts with its [LabelInstruction][com.leko.kvm.bytecode.LabelInstruction].
     * The last instruction is the terminator.
     */
    val instructions: List<Instruction>

    /**
     * The blocks control can reach directly after this one.
     *
     * Empty for [TerminationBlock]s. The list may contain duplicates if several
     * edges lead to the same block, and a new list is built on each access.
     */
    val successors: List<Block>
}

/**
 * A block that ends the method: a [ReturnBlock] or a [ThrowBlock].
 * It has no successors.
 */
sealed interface TerminationBlock : Block

/**
 * A block with no control-flow instruction at the end. It simply falls through
 * to the block that follows it, which usually starts at a label.
 *
 * @property instructions The instructions of this block.
 */
class BasicBlock(
    override val instructions: List<Instruction>,
) : Block {
    /** The block that follows. Assigned during CFG construction. */
    lateinit var successor: Block

    override val successors: List<Block> get() = listOf(successor)
    override fun toString() = "BasicBlock(${instructions.size} instrs)"
}

/**
 * A block ending in a conditional jump, which has two outgoing edges.
 *
 * @property condition The instructions of this block, ending in the conditional jump.
 */
class BranchBlock(
    val condition: List<Instruction>,
) : Block {
    /** The block jumped to when the condition holds. Assigned during CFG construction. */
    lateinit var then: Block

    /** The fall-through block, taken when the condition does not hold. Assigned during CFG construction. */
    lateinit var `else`: Block

    override val instructions: List<Instruction> get() = condition

    /** `[then, else]`, in that order. */
    override val successors: List<Block> get() = listOf(then, `else`)
    override fun toString() = "BranchBlock(${condition.size} instrs)"
}

/**
 * A block ending in an unconditional jump.
 *
 * @property instructions The instructions of this block, ending in the goto.
 */
class GotoBlock(
    override val instructions: List<Instruction>,
) : Block {
    /** The jump target. Assigned during CFG construction. */
    lateinit var next: Block

    override val successors: List<Block> get() = listOf(next)
    override fun toString() = "GotoBlock(${instructions.size} instrs)"
}

/**
 * A block ending in a switch (`tableswitch` or `lookupswitch`).
 *
 * @property dispatch The instructions of this block, ending in the switch.
 */
class SwitchBlock(
    val dispatch: List<Instruction>,
) : Block {
    /** The target block for each case key. Assigned during CFG construction. */
    lateinit var cases: Map<Int, Block>

    /** The block taken when no case matches. Assigned during CFG construction. */
    lateinit var default: Block

    override val instructions: List<Instruction> get() = dispatch

    /**
     * The case targets in map order, followed by [default]. A block that is the
     * target of several cases appears several times.
     */
    override val successors: List<Block> get() = cases.values.toList() + default
    override fun toString() = "SwitchBlock(${dispatch.size} instrs, ${cases.size} cases)"
}

/**
 * A block ending in a return instruction. It is an exit of the method.
 *
 * @property value The instructions of this block, ending in the return.
 */
class ReturnBlock(
    val value: List<Instruction>,
) : TerminationBlock {
    override val instructions: List<Instruction> get() = value
    override val successors: List<Block> = emptyList()
    override fun toString() = "ReturnBlock(${value.size} instrs)"
}

/**
 * A block ending in `athrow`. It is an exit of the method, and its exception
 * is not routed to any handler in this graph.
 *
 * @property throwing The instructions of this block, ending in the throw.
 */
class ThrowBlock(
    val throwing: List<Instruction>,
) : TerminationBlock {
    override val instructions: List<Instruction> = throwing
    override val successors: List<Block> = emptyList()
    override fun toString() = "ThrowBlock(${throwing.size} instrs)"
}