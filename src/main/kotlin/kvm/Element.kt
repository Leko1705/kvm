package com.leko.kvm

/**
 * Root of the hierarchy of program elements that can be declared in a jar:
 * classes ([ClassDeclaration]), methods ([MethodDeclaration]) and fields
 * ([FieldDeclaration]).
 *
 * The hierarchy is sealed so analyses can match over it exhaustively.
 */
sealed interface Element