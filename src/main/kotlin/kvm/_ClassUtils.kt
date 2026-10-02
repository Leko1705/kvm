package com.leko.kvm

/**
 * The constructors of this class, meaning its methods named `<init>`.
 *
 * Returns an empty list for classes with no known constructors, such as a
 * [PhantomClass] that no one has referenced a constructor on.
 */
val ClassDeclaration.constructors: List<MethodDeclaration> get() = this.methods.filter { it.name == "<init>" }