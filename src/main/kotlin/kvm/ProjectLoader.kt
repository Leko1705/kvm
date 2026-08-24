package com.leko.kvm

interface ProjectLoader {

    fun loadProject(config: ProjectConfiguration): Project

}