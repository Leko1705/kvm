package com.leko.kvm.typing

/**
 * A type whose values are references: [ClassType], [ArrayType] and [NullType].
 *
 * Everything else in [Type] is a primitive or `void`.
 */
sealed interface ReferenceType: Type {

    override val javaName: String

    override val jvmName: String

}