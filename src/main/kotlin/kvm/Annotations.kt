package com.leko.kvm

/**
 * An annotation applied to a class, method or field.
 *
 * @property name The annotation type's name.
 * @property values The annotation's element values, keyed by element name.
 * Values are untyped ([Any]); callers need to know the element's declared type.
 */
data class KvmAnnotation(
    val name: String,
    val values: Map<String, Any>
)