package com.leko.kvm.bytecode

import com.leko.kvm.ClassDeclaration


interface BytecodeParser {

    fun parse(bytes: ByteArray): ClassDeclaration

}