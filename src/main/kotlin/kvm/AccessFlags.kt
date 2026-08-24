package com.leko.kvm

sealed interface AccessFlags {
    val bits: Int
}

@JvmInline
value class ClassFlags internal constructor(override val bits: Int) : AccessFlags {
    val isPublic      get() = bits and 0x0001 != 0
    val isProtected   get() = bits and 0x0004 != 0
    val isFinal       get() = bits and 0x0010 != 0
    val isSuper       get() = bits and 0x0020 != 0  // always set in modern JVM
    val isInterface   get() = bits and 0x0200 != 0
    val isAbstract    get() = bits and 0x0400 != 0
    val isSynthetic   get() = bits and 0x1000 != 0
    val isAnnotation  get() = bits and 0x2000 != 0
    val isEnum        get() = bits and 0x4000 != 0

    operator fun plus(other: ClassFlags) = ClassFlags(bits or other.bits)

    operator fun plus(other: Int): ClassFlags = ClassFlags(bits or other)

    operator fun compareTo(other: ClassFlags) = bits.compareTo(other.bits)

    operator fun compareTo(other: Int) = bits.compareTo(other)

    infix fun intersects(other: ClassFlags) = ClassFlags(bits and other.bits)

    infix fun intersects(other: Int) = ClassFlags(bits and other)

    infix fun has(flag: ClassFlags): Boolean = (bits and flag.bits) == flag.bits

    infix fun hasAny(mask: ClassFlags): Boolean = (bits and mask.bits) != 0

    infix fun hasAll(mask: ClassFlags): Boolean = (bits and mask.bits) == mask.bits


    companion object {
        val EMPTY       = ClassFlags(0x0020)  // ACC_SUPER always set
        val PUBLIC      = ClassFlags(0x0001)
        val PROTECTED   = ClassFlags(0x0004)
        val FINAL       = ClassFlags(0x0010)
        val INTERFACE   = ClassFlags(0x0200)
        val ABSTRACT    = ClassFlags(0x0400)
        val SYNTHETIC   = ClassFlags(0x1000)
        val ANNOTATION  = ClassFlags(0x2000)
        val ENUM        = ClassFlags(0x4000)

        val CLASS_SPECIFIC = INTERFACE + ANNOTATION
        val VISIBILITY = PUBLIC + PROTECTED
    }
}

@JvmInline
value class MethodFlags internal constructor(override val bits: Int) : AccessFlags {
    val isPublic        get() = bits and 0x0001 != 0
    val isPrivate       get() = bits and 0x0002 != 0
    val isProtected     get() = bits and 0x0004 != 0
    val isStatic        get() = bits and 0x0008 != 0
    val isFinal         get() = bits and 0x0010 != 0
    val isSynchronized  get() = bits and 0x0020 != 0
    val isBridge        get() = bits and 0x0040 != 0
    val isVarargs       get() = bits and 0x0080 != 0
    val isNative        get() = bits and 0x0100 != 0
    val isAbstract      get() = bits and 0x0400 != 0
    val isSynthetic     get() = bits and 0x1000 != 0

    operator fun plus(other: MethodFlags) = MethodFlags(bits or other.bits)

    operator fun plus(other: Int): MethodFlags = MethodFlags(bits or other)

    operator fun compareTo(other: MethodFlags) = bits.compareTo(other.bits)

    operator fun compareTo(other: Int) = bits.compareTo(other)

    infix fun intersects(other: MethodFlags) = MethodFlags(bits and other.bits)

    infix fun intersects(other: Int) = MethodFlags(bits and other)

    infix fun has(flag: MethodFlags): Boolean = (bits and flag.bits) == flag.bits

    infix fun hasAny(mask: MethodFlags): Boolean = (bits and mask.bits) != 0

    infix fun hasAll(mask: MethodFlags): Boolean = (bits and mask.bits) == mask.bits


    companion object {
        val EMPTY        = MethodFlags(0)
        val PUBLIC       = MethodFlags(0x0001)
        val PRIVATE      = MethodFlags(0x0002)
        val PROTECTED    = MethodFlags(0x0004)
        val STATIC       = MethodFlags(0x0008)
        val FINAL        = MethodFlags(0x0010)
        val SYNCHRONIZED = MethodFlags(0x0020)
        val BRIDGE       = MethodFlags(0x0040)
        val VARARGS      = MethodFlags(0x0080)
        val NATIVE       = MethodFlags(0x0100)
        val ABSTRACT     = MethodFlags(0x0400)
        val SYNTHETIC    = MethodFlags(0x1000)

        val METHOD_SPECIFIC = SYNCHRONIZED + BRIDGE + VARARGS + NATIVE + ABSTRACT
        val VISIBILITY = PUBLIC + PRIVATE + PROTECTED
    }
}

@JvmInline
value class FieldFlags internal constructor(override val bits: Int) : AccessFlags {
    val isPublic     get() = bits and 0x0001 != 0
    val isPrivate    get() = bits and 0x0002 != 0
    val isProtected  get() = bits and 0x0004 != 0
    val isStatic     get() = bits and 0x0008 != 0
    val isFinal      get() = bits and 0x0010 != 0
    val isVolatile   get() = bits and 0x0040 != 0
    val isTransient  get() = bits and 0x0080 != 0
    val isSynthetic  get() = bits and 0x1000 != 0
    val isEnum       get() = bits and 0x4000 != 0

    operator fun plus(other: FieldFlags) = FieldFlags(bits or other.bits)

    operator fun plus(other: Int): FieldFlags = FieldFlags(bits or other)

    operator fun compareTo(other: FieldFlags) = bits.compareTo(other.bits)

    operator fun compareTo(other: Int) = bits.compareTo(other)

    infix fun intersects(other: FieldFlags) = FieldFlags(bits and other.bits)

    infix fun intersects(other: Int) = FieldFlags(bits and other)

    infix fun has(flag: FieldFlags): Boolean = (bits and flag.bits) == flag.bits

    infix fun hasAny(mask: FieldFlags): Boolean = (bits and mask.bits) != 0

    infix fun hasAll(mask: FieldFlags): Boolean = (bits and mask.bits) == mask.bits


    companion object {
        val EMPTY      = FieldFlags(0)
        val PUBLIC     = FieldFlags(0x0001)
        val PRIVATE    = FieldFlags(0x0002)
        val PROTECTED  = FieldFlags(0x0004)
        val STATIC     = FieldFlags(0x0008)
        val FINAL      = FieldFlags(0x0010)
        val VOLATILE   = FieldFlags(0x0040)
        val TRANSIENT  = FieldFlags(0x0080)
        val SYNTHETIC  = FieldFlags(0x1000)
        val ENUM       = FieldFlags(0x4000)

        val FIELD_SPECIFIC = VOLATILE + TRANSIENT
        val VISIBILITY = PUBLIC + PRIVATE + PROTECTED
    }
}
