package com.leko.kvm

/**
 * A [JarFile] entry holding a single compiled class.
 */
interface ClassFile : JarFile.Entry {

    /** The parsed declaration of the class stored in this file. */
    val declaration: ClassDeclaration

}