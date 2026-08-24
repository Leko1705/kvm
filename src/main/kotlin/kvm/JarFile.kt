package com.leko.kvm

interface JarFile {

    sealed interface Entry

    val entries: List<Entry>

}