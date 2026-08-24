package com.leko.kvm.bytecode

import com.leko.kvm.ConcreteClass

interface BytecodeGenerator {

    fun generate(clazz: ConcreteClass): ByteArray

}