package com.leko.kvm.callgraph

import com.leko.kvm.*


@JvmName("callGraphFromClassDeclarations")
suspend fun List<ClassDeclaration>.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = cgType.build(this, spec, entryPoints)

suspend fun ClassDeclaration.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = listOf(this).callGraph(cgType, spec, entryPoints)

@JvmName("callGraphFromClassFiles")
suspend fun List<ClassFile>.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = this.map { it.declaration }.callGraph(cgType, spec, entryPoints)

suspend fun JarFile.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = this.classFiles.callGraph(cgType, spec, entryPoints)

@JvmName("callGraphFromJars")
suspend fun List<JarFile>.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = this.flatMap { it.classFiles }.callGraph(cgType, spec, entryPoints)

suspend fun Project.callGraph(
    cgType: CallGraphType = CHA,
    spec: CallGraphSpecification = NoCallGraphSpecification,
    entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
): CallGraph = this.classFiles.callGraph(cgType, spec, entryPoints)
