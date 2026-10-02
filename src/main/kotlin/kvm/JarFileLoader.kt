package com.leko.kvm

/**
 * Strategy for reading a [JarFile] from a location identified by a string path.
 *
 * Implementations decide how [path] is interpreted (a local file, a remote URL,
 * an in-memory registry key, etc.) and how the jar's contents are parsed.
 * A loader is passed to [loadJar] when adding jars to a [Project].
 *
 * @see JVMJarFileLoader
 */
interface JarFileLoader {

    /**
     * Loads the jar identified by [path].
     *
     * @param path Location of the jar, in whatever form this loader understands.
     * @return The loaded [JarFile].
     */
    fun load(path: String): JarFile

}