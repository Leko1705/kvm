package com.leko.kvm

interface JarFileLoader {

    fun load(path: String): JarFile

}