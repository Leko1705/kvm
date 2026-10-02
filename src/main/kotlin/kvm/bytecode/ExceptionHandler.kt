package com.leko.kvm.bytecode

import com.leko.kvm.typing.ClassType

/**
 * Describes an exception handler in a method body.
 *
 * The handler covers the bytecode region from [start] to [end] and transfers
 * control to [handler] when an exception matching [type] is thrown.
 *
 * A `null` [type] represents a catch-all handler, which is used by the
 * framework for `finally`-style exception handling.
 *
 * @property start the first label of the protected region.
 * @property end the label marking the end of the protected region.
 * @property handler the label at which exception handling begins.
 * @property type the exception type handled, or `null` for a catch-all handler.
 */
data class ExceptionHandler(
    val start: Label,
    val end: Label,
    val handler: Label,
    val type: ClassType?,
)
