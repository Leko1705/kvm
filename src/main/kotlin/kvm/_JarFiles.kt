package com.leko.kvm

import com.leko.kvm.bytecode.ClassBuilderImpl
import com.leko.kvm.bytecode.ClassBuilderScope
import com.leko.kvm.typing.ClassType

@DslMarker
annotation class JarGen

@JarGen
class JarBuilder internal constructor() {

    private data class GeneratedClassFile(override val declaration: ClassDeclaration) : ClassFile
    private data class GeneratedJarFile(override val entries: List<JarFile.Entry>): JarFile

    private val classes = mutableListOf<ClassFile>()

    fun addClass(
        name: String,
        block: ClassBuilderScope.() -> Unit
    ) {
        val builder = ClassBuilderImpl(ClassType(name))
        val scope = ClassBuilderScope(builder)
        scope.apply(block)
        addClass(builder.build())
    }

    fun addClass(clazz: ConcreteClass) {
        classes.add(GeneratedClassFile(clazz))
    }

    internal fun build(): JarFile = GeneratedJarFile(classes)
}


fun JarFile(builder: JarBuilder.() -> Unit): JarFile {
    val builder = JarBuilder()
    builder.builder()
    return builder.build()
}

val JarFile.classFiles: List<ClassFile> get() = this.entries.filterIsInstance<ClassFile>()
