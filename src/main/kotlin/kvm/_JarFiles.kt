package com.leko.kvm

import com.leko.kvm.bytecode.ClassBuilderImpl
import com.leko.kvm.bytecode.ClassBuilderScope
import com.leko.kvm.typing.ClassType

/**
 * DSL marker for the jar generation builders, preventing implicit access to
 * outer builder receivers from nested blocks.
 */
@DslMarker
annotation class JarGen

/**
 * Builder DSL for synthesizing a [JarFile] programmatically.
 *
 * Instances are only created by the [JarFile] builder function (or indirectly
 * through [buildJar]).
 */
@JarGen
class JarBuilder internal constructor() {

    private data class GeneratedClassFile(override val declaration: ClassDeclaration) : ClassFile
    private data class GeneratedJarFile(override val entries: List<JarFile.Entry>): JarFile

    private val classes = mutableListOf<ClassFile>()

    /**
     * Defines a class named [name] using the class builder DSL and adds it to the jar.
     *
     * @param name The name of the class to create, which is wrapped in a [ClassType].
     * @param block Configures the class (members, flags, supertypes, and so on).
     */
    fun addClass(
        name: String,
        block: ClassBuilderScope.() -> Unit
    ) {
        val builder = ClassBuilderImpl(ClassType(name))
        val scope = ClassBuilderScope(builder)
        scope.apply(block)
        addClass(builder.build())
    }

    /**
     * Adds an already constructed [clazz] to the jar.
     *
     * @param clazz The class declaration to wrap in a class file.
     */
    fun addClass(clazz: ConcreteClass) {
        classes.add(GeneratedClassFile(clazz))
    }

    internal fun build(): JarFile = GeneratedJarFile(classes)
}

/**
 * Creates a [JarFile] using the [JarBuilder] DSL.
 *
 * ```kotlin
 * val jar = JarFile {
 *     addClass("com.example.Foo") { /* ... */ }
 * }
 * ```
 *
 * @param builder Configuration block that adds the jar's classes.
 * @return A jar containing every class added in [builder], in the order added.
 */
fun JarFile(builder: JarBuilder.() -> Unit): JarFile {
    val builder = JarBuilder()
    builder.builder()
    return builder.build()
}

/** The [ClassFile] entries of this jar, in entry order. Other entry kinds are skipped. */
val JarFile.classFiles: List<ClassFile> get() = this.entries.filterIsInstance<ClassFile>()