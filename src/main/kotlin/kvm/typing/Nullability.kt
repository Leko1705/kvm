package com.leko.kvm.typing

@JvmInline
value class Nullability(val value: Int) {
    companion object {
        val UNKNOWN = Nullability(0)   // no annotation found
        val NULLABLE = Nullability(1)  // @Nullable present
        val NOT_NULL = Nullability(2)  // @NotNull present
    }
}
