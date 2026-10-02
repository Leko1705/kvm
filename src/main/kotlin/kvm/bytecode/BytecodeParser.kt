package com.leko.kvm.bytecode

import com.leko.kvm.ClassDeclaration


/**
 * Parses JVM class-file bytecode into a [ClassDeclaration].
 *
 * Implementations may use a bytecode library such as ASM to translate the
 * binary class-file representation.
 */
interface BytecodeParser {

    /**
     * Parses a JVM class file.
     *
     * @param bytes the binary class-file representation.
     * @return the parsed class declaration.
     */
    fun parse(bytes: ByteArray): ClassDeclaration
}