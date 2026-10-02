package com.leko.kvm


val ClassDeclaration.constructors: List<MethodDeclaration> get() = this.methods.filter { it.name == "<init>" }
