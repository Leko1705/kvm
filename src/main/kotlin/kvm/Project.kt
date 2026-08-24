package com.leko.kvm

@ConsistentCopyVisibility
data class Project internal constructor(
    val jars: List<JarFile>
)

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

interface ProjectBuilder {
    fun jar(jar: JarFile)
}

fun ProjectBuilder.buildJar(builder: JarBuilder.() -> Unit) {
    jar(JarFile(builder))
}

fun ProjectBuilder.loadJar(path: String, loader: JarFileLoader = JVMJarFileLoader) {
    val jarFile = loader.load(path)
    jar(jarFile)
}
