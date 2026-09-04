package com.leko.kvm.bytecode

import com.leko.kvm.ConcreteClass
import com.leko.kvm.MethodBody
import com.leko.kvm.typing.ClassType


fun ConcreteClass.bytes(generator: BytecodeGenerator = ASM): ByteArray =
    generator.generate(this)

fun JBCTree.toMethodBody(autoTerminate: Boolean = true): MethodBody {
    val instructions = mutableListOf<Instruction>()
    val exceptionHandlers = mutableListOf<ExceptionHandler>()

    class Generator: InstructionGenerator {
        override fun emit(instruction: Instruction) {
            instructions.add(instruction)
        }
        override fun placeLabel(label: Label) {
            instructions.add(LabelInstruction(label))
        }
        override fun registerExceptionHandler(start: Label, end: Label, handler: Label, type: ClassType?) {
            exceptionHandlers.add(ExceptionHandler(start, end, handler, type))
        }
    }

    this.generate(Generator())
    if (autoTerminate) {
        val lastInst = instructions.lastOrNull()
        if (lastInst !is ReturnInstruction && lastInst !is AThrowInstruction) {
            instructions.add(VReturnInstruction)
        }
    }
    return MethodBody(instructions, exceptionHandlers)
}

fun List<Instruction>.readable(): List<String> = map { it.readable() }
