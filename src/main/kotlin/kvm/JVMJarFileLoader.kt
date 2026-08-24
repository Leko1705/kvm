package com.leko.kvm

import com.leko.kvm.bytecode.ASM
import java.io.Closeable
import java.io.File
import java.util.zip.ZipEntry
import java.util.jar.JarFile as JdkJarFile

object JVMJarFileLoader : JarFileLoader {

    override fun load(path: String): JarFile {
        val jdkJarFile = JdkJarFile(File(path))

        val classEntries = jdkJarFile.entries().asSequence()
            .filter { !it.isDirectory && it.name.endsWith(".class") }
            .toList()

        return LazyJarFile(jdkJarFile, classEntries)
    }
}

private class LazyJarFile(
    private val jdkJarFile: JdkJarFile,
    zipEntries: List<ZipEntry>,
) : JarFile, Closeable {

    override val entries: List<JarFile.Entry> = zipEntries.map { entry ->
        LazyClassFile(jdkJarFile, entry)
    }

    override fun close() = jdkJarFile.close()
}

private class LazyClassFile(
    private val jdkJarFile: JdkJarFile,
    private val entry: ZipEntry,
) : ClassFile {

    // Only read + parse bytecode the first time this is touched
    override val declaration: ClassDeclaration by lazy {
        val bytes = jdkJarFile.getInputStream(entry).use { it.readBytes() }
        ASM.parse(bytes)
    }

}