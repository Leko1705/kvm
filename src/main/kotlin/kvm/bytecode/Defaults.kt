package com.leko.kvm.bytecode

import com.leko.kvm.JVMJarFileLoader
import com.leko.kvm.JarFileLoader


/** Returns the default platform specific [BytecodeConverter]. */
fun defaultConverter(): BytecodeConverter = ASM

/** Returns the default platform specific [BytecodeGenerator]. */
fun defaultGenerator(): BytecodeGenerator = defaultConverter()

/** Returns the default platform specific [BytecodeParser]. */
fun defaultParser(): BytecodeParser = defaultConverter()

/** Returns the default platform specific [JarFileLoader]. */
fun defaultJarFileLoader(): JarFileLoader = JVMJarFileLoader
