package com.leko.kvm.callgraph

import com.leko.kvm.*


/**
 * Builds a call graph of these [ClassDeclaration]s using [cgType].
 *
 * Only these classes are analyzed. Everything else they reference is phantom.
 *
 * @param cgType The algorithm, [CHA] by default.
 * @param spec Controls exploration and which edges are kept.
 * @param entryPoints Selects the methods to start from. Defaults to public methods;
 * see also [mainMethods].
 * @see CallGraphType.build
 */
@JvmName("callGraphFromClassDeclarations")
suspend fun List<ClassDeclaration>.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = cgType.build(this, spec, entryPoints)

/**
 * Builds a call graph of this single class.
 *
 * Calls to any other class resolve to phantom methods, so this is mostly
 * useful for intra-class analysis.
 *
 * @see CallGraphType.build
 */
suspend fun ClassDeclaration.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = listOf(this).callGraph(cgType, spec, entryPoints)

/**
 * Builds a call graph of the declarations of these [ClassFile]s.
 *
 * @see CallGraphType.build
 */
@JvmName("callGraphFromClassFiles")
suspend fun List<ClassFile>.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = this.map { it.declaration }.callGraph(cgType, spec, entryPoints)

/**
 * Builds a call graph of the classes in this jar.
 *
 * Classes in other jars are not visible. To resolve calls across jars, use a
 * [Project] or a list of jars instead.
 *
 * @see CallGraphType.build
 */
suspend fun JarFile.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = this.classFiles.callGraph(cgType, spec, entryPoints)

/**
 * Builds a call graph of the classes of all these jars together, so calls and
 * inheritance across jars are resolved.
 *
 * @see CallGraphType.build
 */
@JvmName("callGraphFromJars")
suspend fun List<JarFile>.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = this.flatMap { it.classFiles }.callGraph(cgType, spec, entryPoints)

/**
 * Builds a call graph of every class in this project, the usual entry point
 * for whole-program analysis.
 *
 * @see CallGraphType.build
 */
suspend fun Project.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = this.classFiles.callGraph(cgType, spec, entryPoints)