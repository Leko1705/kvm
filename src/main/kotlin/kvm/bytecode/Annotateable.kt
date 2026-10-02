package com.leko.kvm.bytecode


/**
 * A bytecode element that can have annotations attached to it.
 *
 * Implementations use [annotation] to create an annotation builder for a
 * specific annotation type. The returned builder can then be used to define
 * the annotation's element values.
 */
interface Annotateable {

    /**
     * Creates a builder for an annotation with the specified type name.
     *
     * The annotation is attached to this element when the annotation builder
     * is created. Its element values can subsequently be populated through
     * [AnnotationBuilder.put].
     *
     * @param name the fully qualified name of the annotation type.
     * @return a builder for configuring the annotation.
     */
    fun annotation(name: String): AnnotationBuilder
}