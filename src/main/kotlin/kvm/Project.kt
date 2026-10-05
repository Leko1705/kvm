package com.leko.kvm

import com.leko.kvm.bytecode.defaultJarFileLoader

/**
 * A collection of arbitrary analyzable [JarFile]s, forming
 * an inter-jar global context.
 *
 * A [Project] is the unit of whole-program analysis: anything that needs to
 * resolve references across jar boundaries (e.g. a class in one jar extending
 * a class in another) operates on a project rather than on a single jar.
 *
 * The primary constructor is `internal`. Use the [Project] builder function
 * to create instances from outside this module:
 *
 * ```kotlin
 * val project = Project {
 *     loadJar("libs/app.jar")
 *     loadJar("libs/dependency.jar")
 *     buildJar { /* synthesize a jar programmatically */ }
 * }
 * ```
 *
 * @property jars The jars that make up this project, in the order they were
 * added.
 */
@ConsistentCopyVisibility
data class Project internal constructor(
    val jars: List<JarFile>
)

/**
 * Creates a [Project] using a type-safe builder DSL.
 *
 * The [builder] block is invoked with a [ProjectBuilder] receiver. Each call
 * to [ProjectBuilder.jar] (or one of its extensions, [buildJar] and [loadJar])
 * appends a jar to the resulting project, preserving call order.
 *
 * @param builder Configuration block that registers the project's jars.
 * @return A new [Project] containing every jar registered in [builder].
 */
fun Project(
    builder: ProjectBuilder.() -> Unit
): Project {

    val jars = mutableListOf<JarFile>()

    class Impl: ProjectBuilder {
        override fun jar(jar: JarFile) {
            jars.add(jar)
        }
    }

    val impl = Impl()
    impl.builder()

    return Project(jars)
}

/**
 * Receiver of the [Project] builder DSL.
 *
 * Implementations collect the jars registered during the builder block.
 * Prefer the extension functions [buildJar] and [loadJar] for common cases.
 */
interface ProjectBuilder {
    /**
     * Adds an existing [jar] to the project being built.
     *
     * @param jar The jar to add.
     */
    fun jar(jar: JarFile)
}

/**
 * Builds a [JarFile] with the given [builder] and adds it to the project.
 *
 * Useful for synthesizing jars programmatically rather than reading them
 * from disk.
 *
 * @param builder Configuration block applied to a [JarBuilder].
 */
fun ProjectBuilder.buildJar(builder: JarBuilder.() -> Unit) {
    jar(JarFile(builder))
}

/**
 * Loads a jar from [path] using [loader] and adds it to the project.
 *
 * @param path Location of the jar, interpreted by [loader]
 * (by default, a path on the local file system).
 * @param loader Strategy used to read and parse the jar. Defaults to
 * [defaultJarFileLoader].
 */
fun ProjectBuilder.loadJar(path: String, loader: JarFileLoader = defaultJarFileLoader()) {
    val jarFile = loader.load(path)
    jar(jarFile)
}