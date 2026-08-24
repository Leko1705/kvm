package com.leko.kvm.bytecode

import com.leko.kvm.typing.ClassType

data class ExceptionHandler(
    val start: Label,
    val end: Label,
    val handler: Label,
    val type: ClassType?,
)
