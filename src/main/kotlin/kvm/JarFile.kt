package com.leko.kvm

/**
 * An analyzable jar archive: an ordered collection of [Entry]s.
 *
 * Jars are grouped into a [Project] to form a global analysis context, and are
 * typically obtained through a [JarFileLoader] or [buildJar].
 */
interface JarFile {

    /**
     * An item stored in a [JarFile].
     *
     * The hierarchy is sealed, so the supported kinds of entries are known to the
     * library. The currently known kind is [ClassFile].
     */
    sealed interface Entry

    /** The entries of this jar. */
    val entries: List<Entry>

}