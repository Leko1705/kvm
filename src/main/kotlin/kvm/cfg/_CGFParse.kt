package com.leko.kvm.cfg

import com.leko.kvm.bytecode.*
import com.leko.kvm.ConcreteMethod
import com.leko.kvm.MethodBody

/**
 * Builds a [ControlFlowGraph] from a linear list of bytecode instructions.
 *
 * Construction has two phases:
 *
 * 1. **Leader identification.** An instruction starts a new basic block if it is
 *    the first instruction, a [LabelInstruction], or the instruction after a
 *    jump, switch, return or throw. Jump and switch targets are leaders as well.
 * 2. **Block construction and edge wiring.** The instructions are split at the
 *    leaders. Each slice becomes a [Block] whose type is determined by its last
 *    instruction. Successors are wired in a second pass, after every block
 *    exists, so loops and back-edges resolve correctly.
 *
 * Only normal control flow is modeled. Exception handlers are not considered,
 * so no edges lead into handler code (see [MethodBody.handlers]).
 *
 * @receiver The instructions of a single method body, in bytecode order.
 * @return The control flow graph, whose [entry][ControlFlowGraph.entry] is the
 * block containing the first instruction.
 * @throws IllegalArgumentException if the instruction list is empty.
 * @throws IllegalStateException if a jump or switch targets a label with no
 * matching [LabelInstruction], or if execution can fall off the end of the
 * list without a return or throw.
 */
fun List<Instruction>.cfg(): ControlFlowGraph {
    val instrs = this
    require(instrs.isNotEmpty()) { "Cannot build a CFG from an empty instruction list" }

    // ── Phase 1: leader identification ──────────────────────────────────────

    val labelIndex = HashMap<Label, Int>()
    for ((i, instr) in instrs.withIndex()) {
        if (instr is LabelInstruction) labelIndex[instr.label] = i
    }

    fun indexOf(label: Label): Int =
        labelIndex[label] ?: error("Unresolved jump target: no LabelInstruction for $label")

    val leaders = sortedSetOf(0)

    for ((i, instr) in instrs.withIndex()) {
        val next = i + 1
        when (instr) {
            is LabelInstruction -> leaders += i

            is GotoInstruction -> {
                leaders += indexOf(instr.target)
                if (next < instrs.size) leaders += next
            }

            is JumpInstruction -> { // conditional if* (has a fallthrough)
                leaders += indexOf(instr.target)
                if (next < instrs.size) leaders += next
            }

            is SwitchInstruction -> {
                leaders += indexOf(instr.default)
                instr.cases.values.forEach { leaders += indexOf(it) }
                if (next < instrs.size) leaders += next
            }

            is ReturnInstruction, is AThrowInstruction -> {
                if (next < instrs.size) leaders += next
            }

            else -> Unit
        }
    }

    val leaderList = leaders.toList()

    // (start, endExclusive) ranges — one per basic block
    val ranges = (leaderList + instrs.size)
        .windowed(size = 2)
        .map { it.first() to it.last() }

    // ── Phase 2: block construction + edge wiring ───────────────────────────

    // Pass 2a: create block shells (successors not wired yet — needed for cycles)
    val blockByStart = LinkedHashMap<Int, Block>()
    for ((start, end) in ranges) {
        val slice = instrs.subList(start, end)
        val terminator = slice.last()
        blockByStart[start] = when (terminator) {
            is GotoInstruction -> GotoBlock(slice)
            is JumpInstruction -> BranchBlock(slice)
            is SwitchInstruction -> SwitchBlock(slice)
            is ReturnInstruction -> ReturnBlock(slice)
            is AThrowInstruction -> ThrowBlock(slice)
            else -> BasicBlock(slice) // falls through to the next block
        }
    }

    fun blockAt(start: Int): Block =
        blockByStart[start] ?: error("No block starts at instruction index $start")

    // Pass 2b: back-patch successors now that every block exists
    for ((start, end) in ranges) {
        val block = blockByStart.getValue(start)
        val terminator = instrs[end - 1]
        when (block) {
            is BasicBlock -> {
                check(end < instrs.size) {
                    "Fell off the end of the instruction list without a return/throw (block at $start)"
                }
                block.successor = blockAt(end)
            }
            is GotoBlock -> {
                block.next = blockAt(indexOf((terminator as GotoInstruction).target))
            }
            is BranchBlock -> {
                val jump = terminator as JumpInstruction
                block.then = blockAt(indexOf(jump.target))
                block.`else` = blockAt(end) // fallthrough — guaranteed adjacent, see below
            }
            is SwitchBlock -> {
                val sw = terminator as SwitchInstruction
                block.cases = sw.cases.mapValues { (_, label) -> blockAt(indexOf(label)) }
                block.default = blockAt(indexOf(sw.default))
            }
            is ReturnBlock, is ThrowBlock -> Unit // no successors
        }
    }

    val entry = blockAt(0)
    val all = blockByStart.values.toList()
    val exits = all.filterIsInstance<TerminationBlock>()

    return CFGImpl(entry = entry, exits = exits, all = all)
}

/**
 * Builds a [ControlFlowGraph] from this body's [instructions][MethodBody.instructions].
 *
 * @see List.cfg
 */
fun MethodBody.cfg(): ControlFlowGraph = instructions.cfg()

/**
 * Builds a [ControlFlowGraph] from this method's [body][ConcreteMethod.body].
 *
 * Accessing this forces the lazy body to be computed.
 *
 * @see List.cfg
 */
fun ConcreteMethod.cfg(): ControlFlowGraph = body.cfg()
