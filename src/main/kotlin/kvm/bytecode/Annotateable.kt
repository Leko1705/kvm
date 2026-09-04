package com.leko.kvm.bytecode

interface Annotateable {

    fun annotation(name: String): AnnotationBuilder

}