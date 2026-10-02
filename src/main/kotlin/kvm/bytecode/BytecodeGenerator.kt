package com.leko.kvm.bytecode

import com.leko.kvm.ConcreteClass

/**
 * Generates JVM class-file bytecode from the framework's class model.
 *
 * A generator is responsible for translating a [ConcreteClass] into its
 * binary JVM class-file representation.
 */
interface BytecodeGenerator {

    /**
     * Generates a JVM class file.
     *
     * @param clazz the class model to generate.
     * @return the generated class-file bytes.
     */
    fun generate(clazz: ConcreteClass): ByteArray
}