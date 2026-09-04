package com.leko.kvm.callgraph

import com.leko.kvm.ClassDeclaration
import com.leko.kvm.ClassFile
import com.leko.kvm.ConcreteClass
import com.leko.kvm.ConcreteMethod
import com.leko.kvm.JarFile
import com.leko.kvm.MethodDeclaration
import com.leko.kvm.MethodSignature
import com.leko.kvm.typing.ClassHierarchy
import com.leko.kvm.bytecode.*
import com.leko.kvm.typing.ArrayType
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.VoidType
import com.leko.kvm.Project
import com.leko.kvm.classFiles
import com.leko.kvm.parameterTypes
import com.leko.kvm.returnType

private fun buildCha(classFiles: List<ClassDeclaration>, entryPoints: Set<MethodSignature>): CallGraph {
    val hierarchy = ClassHierarchy(classFiles)
    val bySignature = classFiles
        .flatMap { (it as? ConcreteClass)?.methods.orEmpty().asSequence() }
        .associateBy { it.signature }

    val entryDecls = entryPoints.mapNotNull { bySignature[it] }.toSet()
    val visited = mutableSetOf<MethodDeclaration>()
    val edges = mutableListOf<CallEdge>()
    val worklist = ArrayDeque<MethodDeclaration>().apply { addAll(entryDecls) }

    while (worklist.isNotEmpty()) {
        val method = worklist.removeFirst()
        if (!visited.add(method) || method !is ConcreteMethod) continue

        for (instr in method.body.instructions) {
            if (instr !is InvocationInstruction) continue
            val site = CallSite(method, instr)
            for (target in resolveTargets(hierarchy, instr, instantiatedTypes = null)) {
                edges += CallEdge(site, target)
                worklist.add(target)
            }
        }
    }

    return CallGraphImpl(entryDecls, edges)
}

private fun buildRta(classFiles: List<ClassDeclaration>, entryPoints: Set<MethodSignature>): CallGraph {
    val hierarchy = ClassHierarchy(classFiles)
    val bySignature = classFiles
        .flatMap { (it as? ConcreteClass)?.methods.orEmpty().asSequence() }
        .associateBy { it.signature }

    val entryDecls = entryPoints.mapNotNull { bySignature[it] }.toSet()
    var reachable: Set<MethodDeclaration> = entryDecls
    var instantiated: Set<ClassType> = emptySet()

    while (true) {
        val concreteReachable = reachable.filterIsInstance<ConcreteMethod>()

        val newInstantiated = instantiated + concreteReachable
            .flatMap { it.body.instructions }
            .filterIsInstance<NewInstruction>()
            .map { it.type }

        val newReachable = reachable + concreteReachable
            .flatMap { it.body.instructions }
            .filterIsInstance<InvocationInstruction>()
            .flatMap { resolveTargets(hierarchy, it, newInstantiated) }

        if (newReachable.size == reachable.size && newInstantiated.size == instantiated.size) {
            reachable = newReachable; instantiated = newInstantiated
            break
        }
        reachable = newReachable
        instantiated = newInstantiated
    }

    val edges = reachable.filterIsInstance<ConcreteMethod>().flatMap { method ->
        method.body.instructions.filterIsInstance<InvocationInstruction>().flatMap { instr ->
            resolveTargets(hierarchy, instr, instantiated).map { CallEdge(CallSite(method, instr), it) }
        }
    }

    return CallGraphImpl(entryDecls, edges)
}

private fun resolveTargets(
    hierarchy: ClassHierarchy,
    instruction: InvocationInstruction,
    instantiatedTypes: Set<ClassType>?, // null = CHA (no filtering), non-null = RTA
): List<MethodDeclaration> = when (instruction) {
    is DirectInvocationInstruction ->
        listOf(hierarchy.resolveMethod(instruction.owner, instruction.method))

    is IndirectInvocationInstruction -> {
        val candidates = hierarchy.subtypes(instruction.owner)
        val filtered = instantiatedTypes?.let { candidates.filter { t -> t in it } } ?: candidates
        // fall back to unfiltered if RTA filters out everything (e.g. an interface type itself)
        (filtered.ifEmpty { candidates })
            .map { hierarchy.resolveMethod(it, instruction.method) }
            .distinct()
    }

    is InvokeDynamicInstruction -> emptyList() // see note below
}

fun List<ClassDeclaration>.mainMethods(): Set<MethodSignature> =
    this
        .flatMap { (it as? ConcreteClass)?.methods.orEmpty().asSequence() }
        .map { it.signature }
        .filter {
            it.name == "main"
                    && it.returnType == VoidType
                    && it.parameterTypes == listOf(ArrayType(ClassType("java/lang/String")))
        }
        .toSet()

fun List<ClassDeclaration>.publicApiMethods(): Set<MethodSignature> =
    this
        .flatMap { (it as? ConcreteClass)?.methods.orEmpty().asSequence() }
        .filter { it.accessFlags.isPublic } // + probably: owning class is also public
        .map { it.signature }
        .toSet()


@JvmName("callGraphFromClassDeclarations")
fun List<ClassDeclaration>.callGraph(
    cgType: CallGraphType = CallGraphType.ClassHierarchyAnalysis(publicApiMethods())
): CallGraph {
    return when (cgType) {
        is CallGraphType.ClassHierarchyAnalysis -> buildCha(this, cgType.entryPoints)
        is CallGraphType.RapidTypeAnalysis -> buildRta(this, cgType.entryPoints)
    }
}

fun ClassDeclaration.callGraph(
    cgType: CallGraphType = CallGraphType.ClassHierarchyAnalysis(listOf(this).publicApiMethods())
): CallGraph = listOf(this).callGraph(cgType)

@JvmName("callGraphFromClassFiles")
fun List<ClassFile>.callGraph(
    cgType: CallGraphType = CallGraphType.ClassHierarchyAnalysis(this.map { it.declaration }.publicApiMethods())
): CallGraph = this.map { it.declaration }.callGraph(cgType)

fun JarFile.callGraph(
    cgType: CallGraphType = CallGraphType.ClassHierarchyAnalysis(this.classFiles.map { it.declaration }.publicApiMethods())
): CallGraph = this.classFiles.callGraph(cgType)

@JvmName("callGraphFromJars")
fun List<JarFile>.callGraph(
    cgType: CallGraphType = CallGraphType.ClassHierarchyAnalysis(this.flatMap { it.classFiles }.map { it.declaration }.publicApiMethods())
): CallGraph = this.flatMap { it.classFiles }.callGraph(cgType)

fun Project.callGraph(
    cgType: CallGraphType = CallGraphType.ClassHierarchyAnalysis(this.classFiles.map { it.declaration }.publicApiMethods())
): CallGraph = this.classFiles.callGraph(cgType)
