package com.leko.kvm

interface ClassFile : JarFile.Entry {

    val declaration: ClassDeclaration

}