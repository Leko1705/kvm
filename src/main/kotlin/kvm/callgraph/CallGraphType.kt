package com.leko.kvm.callgraph

import com.leko.kvm.ClassDeclaration
import com.leko.kvm.ConcreteMethod
import com.leko.kvm.MethodDeclaration
import com.leko.kvm.PresentMethodDeclaration
import com.leko.kvm.bytecode.DirectInvocationInstruction
import com.leko.kvm.bytecode.IndirectInvocationInstruction
import com.leko.kvm.bytecode.InvocationInstruction
import com.leko.kvm.bytecode.InvokeDynamicInstruction
import com.leko.kvm.bytecode.NewInstruction
import com.leko.kvm.parameterTypes
import com.leko.kvm.typing.ClassHierarchy
import com.leko.kvm.typing.ClassType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.collections.filterIsInstance


interface CallGraphType {

    suspend fun build(
        classFiles: List<ClassDeclaration>,
        spec: CallGraphSpecification = NoCallGraphSpecification,
        entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
    ): CallGraph

}

data object CHA : CallGraphType {

    override suspend fun build(
        classFiles: List<ClassDeclaration>,
        spec: CallGraphSpecification,
        entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration>,
    ): CallGraph = coroutineScope {

        val hierarchy = ClassHierarchy(classFiles)

        val entryDecls = classFiles.entryPoints()
            .filterIsInstance<PresentMethodDeclaration>()
            .toSet()

        if (spec is EmptyCallGraphSpecification) {
            return@coroutineScope CallGraphImpl(entryDecls, emptyList())
        }

        val visited = HashSet<ConcreteMethod>()
        val edges = LinkedHashSet<CallEdge>()

        var frontier: List<ConcreteMethod> = entryDecls
            .filterIsInstance<ConcreteMethod>()
            .filter { spec.shouldExpand(it) && visited.add(it) }

        while (frontier.isNotEmpty()) {
            val next = ArrayList<ConcreteMethod>()

            val results = frontier
                .map { method ->
                    async(Dispatchers.Default) {
                        scanMethod(hierarchy, method, spec)
                    }
                }
                .awaitAll()

            for (methodEdges in results) {
                for (edge in methodEdges) {
                    edges += edge
                    val callee = edge.callee
                    if (callee is ConcreteMethod && spec.shouldExpand(callee) && visited.add(callee)) {
                        next += callee
                    }
                }
            }

            frontier = next
        }

        CallGraphImpl(entryDecls, edges.toList())
    }

    private suspend fun scanMethod(
        hierarchy: ClassHierarchy,
        method: ConcreteMethod,
        spec: CallGraphSpecification,
    ): List<CallEdge> {
        val result = ArrayList<CallEdge>()
        for (instr in method.body.instructions) {
            if (instr !is InvocationInstruction) continue
            val site = CallSite(method, instr)
            if (!spec.shouldResolve(site)) continue
            for (target in resolveTargets(hierarchy, instr)) {
                val edge = CallEdge(site, target)
                if (!spec.shouldInclude(edge)) continue
                result += edge
            }
        }
        return result
    }

    private fun resolveTargets(
        hierarchy: ClassHierarchy,
        instruction: InvocationInstruction,
    ): List<MethodDeclaration> = when (instruction) {
        is DirectInvocationInstruction ->
            listOf(hierarchy.resolveMethod(instruction.owner, instruction.method))
        is IndirectInvocationInstruction ->
            hierarchy.subtypes(instruction.owner)
                .map { hierarchy.resolveMethod(it, instruction.method) }
                .distinct()
        is InvokeDynamicInstruction -> emptyList()
    }
}


data object RTA : CallGraphType {

    private class RtaScan(
        val instantiated: Set<ClassType>,
        val directEdges: List<CallEdge>,
        val virtualSites: List<CallSite>,
    )

    override suspend fun build(
        classFiles: List<ClassDeclaration>,
        spec: CallGraphSpecification,
        entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration>,
    ): CallGraph = coroutineScope {

        val hierarchy = ClassHierarchy(classFiles)

        val entryDecls = classFiles.entryPoints()
            .filterIsInstance<PresentMethodDeclaration>()
            .toSet()

        if (spec is EmptyCallGraphSpecification) {
            return@coroutineScope CallGraphImpl(entryDecls, emptyList())
        }

        val visited = HashSet<ConcreteMethod>()
        val edges = LinkedHashSet<CallEdge>()
        val instantiated = HashSet<ClassType>()
        val pendingVirtual = HashMap<ClassType, MutableList<CallSite>>()  // static receiver type -> sites
        var next = ArrayList<ConcreteMethod>()

        suspend fun addEdge(site: CallSite, target: MethodDeclaration) {
            val edge = CallEdge(site, target)
            if (!spec.shouldInclude(edge)) return
            edges += edge
            if (target is ConcreteMethod && spec.shouldExpand(target) && visited.add(target)) {
                next += target
            }
        }

        suspend fun onInstantiated(type: ClassType) {
            if (!instantiated.add(type)) return
            for (owner in hierarchy.supertypes(type, inclusive = true)) {
                for (site in pendingVirtual[owner].orEmpty()) {
                    val call = site.instruction as IndirectInvocationInstruction
                    addEdge(site, hierarchy.resolveMethod(type, call.method))
                }
            }
        }

        suspend fun onVirtualSite(site: CallSite) {
            val call = site.instruction as IndirectInvocationInstruction
            pendingVirtual.getOrPut(call.owner) { mutableListOf() } += site
            for (type in hierarchy.subtypes(call.owner)) {
                if (type in instantiated) addEdge(site, hierarchy.resolveMethod(type, call.method))
            }
        }

        for (entry in entryDecls) {
            for (param in entry.parameterTypes) {
                if (param is ClassType) hierarchy.subtypes(param).forEach(instantiated::add)
            }
            instantiated += entry.owner
        }

        var frontier: List<ConcreteMethod> = entryDecls
            .filterIsInstance<ConcreteMethod>()
            .filter { spec.shouldExpand(it) && visited.add(it) }

        while (frontier.isNotEmpty()) {
            next = ArrayList()

            val scans = frontier
                .map { method -> async(Dispatchers.Default) { scanMethod(hierarchy, method, spec) } }
                .awaitAll()

            for (scan in scans) {
                for (instance in scan.instantiated) onInstantiated(instance)
                scan.directEdges.forEach { addEdge(it.site, it.callee) }
                for (site in scan.virtualSites) onVirtualSite(site)
            }
            frontier = next
        }

        CallGraphImpl(entryDecls, edges.toList())
    }

    private suspend fun scanMethod(hierarchy: ClassHierarchy, method: ConcreteMethod, spec: CallGraphSpecification): RtaScan {
        val types = HashSet<ClassType>()
        val direct = ArrayList<CallEdge>()
        val virtual = ArrayList<CallSite>()

        for (instr in method.body.instructions) {
            when (instr) {
                is NewInstruction -> types += instr.type
                is DirectInvocationInstruction -> {
                    val site = CallSite(method, instr)
                    if (!spec.shouldResolve(site)) continue
                    direct += CallEdge(site, hierarchy.resolveMethod(instr.owner, instr.method))
                }
                is IndirectInvocationInstruction -> {
                    val site = CallSite(method, instr)
                    if (spec.shouldResolve(site)) virtual += site
                }
                is InvokeDynamicInstruction -> Unit
                else -> Unit
            }
        }
        return RtaScan(types, direct, virtual)
    }
}
