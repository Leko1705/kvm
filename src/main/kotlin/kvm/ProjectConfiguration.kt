package com.leko.kvm

data class ProjectConfiguration(
    val jars: List<String>,
    val loadedJars: List<JarFile>
)
