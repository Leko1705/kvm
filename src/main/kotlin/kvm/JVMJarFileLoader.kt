package com.leko.kvm

import com.leko.kvm.bytecode.defaultParser
import java.io.Closeable
import java.io.File
import java.util.zip.ZipEntry
import java.util.jar.JarFile as JdkJarFile

/**
 * A [JarFileLoader] that reads jars from the local file system using the
 * JDK's jar support.
 *
 * The jar is opened and its entry table is read eagerly, but class contents
 * are not parsed here. The returned [LazyJarFile] holds on to the opened
 * archive and the list of `.class` entries so that classes can be read
 * on demand.
 */
object JVMJarFileLoader : JarFileLoader {

    /**
     * Opens the jar at [path] and indexes its class entries.
     *
     * @param path File system path of the jar to open.
     * @return A [LazyJarFile] backed by the opened archive. The underlying
     * archive stays open for the lifetime of the returned object.
     */
    override fun load(path: String): JarFile {
        val jdkJarFile = JdkJarFile(File(path))

        val classEntries = jdkJarFile.entries().asSequence()
            .filter { !it.isDirectory && it.name.endsWith(".class") }
            .toList()

        return LazyJarFile(path, jdkJarFile, classEntries)
    }
}

private class LazyJarFile(
    private val path: String,
    private val jdkJarFile: JdkJarFile,
    zipEntries: List<ZipEntry>,
) : JarFile, Closeable {

    override val entries: List<JarFile.Entry> = zipEntries.map { entry ->
        LazyClassFile(jdkJarFile, entry)
    }

    override fun close() = jdkJarFile.close()

    override fun toString(): String = path
}

private class LazyClassFile(
    private val jdkJarFile: JdkJarFile,
    private val entry: ZipEntry,
) : ClassFile {

    // Only read + parse bytecode the first time this is touched
    override val declaration: ClassDeclaration by lazy {
        val bytes = jdkJarFile.getInputStream(entry).use { it.readBytes() }
        defaultParser().parse(bytes)
    }

}