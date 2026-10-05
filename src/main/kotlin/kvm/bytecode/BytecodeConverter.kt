package com.leko.kvm.bytecode

/**
 * A single object that can both read and write class files
 *
 * @param generator Handles everything declared by [BytecodeGenerator].
 * @param parser Handles everything declared by [BytecodeParser].
 * @see DistinctBytecodeConverter
 */
interface BytecodeConverter: BytecodeGenerator, BytecodeParser

/**
 * A single object that can both read and write class files, composed from a
 * separate [BytecodeGenerator] and [BytecodeParser].
 *
 * Every member of [BytecodeGenerator] is forwarded to [generator], and every
 * member of [BytecodeParser] to [parser]. The converter adds no behavior of its
 * own. It exists so that code needing both directions (for example, loading a
 * class, transforming it and writing it back) can take one dependency instead of two.
 *
 * The two halves are independent and need not share a backend. Pass different
 * implementations to read with one library and write with another.
 *
 * ```kotlin
 * val converter = DistinctBytecodeConverter(generator = ASM, parser = ASM)
 * ```
 *
 * @param generator Handles everything declared by [BytecodeGenerator].
 * @param parser Handles everything declared by [BytecodeParser].
 * @see defaultConverter
 */
class DistinctBytecodeConverter(
    generator: BytecodeGenerator,
    parser: BytecodeParser,
): BytecodeConverter, BytecodeGenerator by generator, BytecodeParser by parser
