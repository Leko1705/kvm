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


/**
 * An algorithm for building a [CallGraph].
 *
 * Available implementations are [CHA] (class hierarchy analysis, cheaper and more
 * conservative) and [RTA] (rapid type analysis, more precise). Both work
 * on demand: only methods reachable from the entry points are analyzed.
 */
interface CallGraphType {

    /**
     * Builds the call graph of [classFiles].
     *
     * Classes outside [classFiles] are treated as unknown, so calls into them
     * resolve to phantom methods and are never expanded.
     *
     * @param classFiles The analyzed classes. They also define the class hierarchy
     * used to resolve virtual calls (despite the name, these are declarations).
     * @param spec Controls which methods are expanded, which call sites are
     * resolved and which edges are recorded. Defaults to [NoCallGraphSpecification].
     * @param entryPoints Selects the methods to start from, given [classFiles] as receiver.
     * Anything that is not a [com.leko.kvm.PresentMethodDeclaration] is dropped.
     * Only concrete entry points are expanded. Defaults to all public methods.
     * @return The resulting graph. [CallGraph.entryPoints] holds the selected entry points.
     */
    suspend fun build(
        classFiles: List<ClassDeclaration>,
        spec: CallGraphSpecification = NoCallGraphSpecification,
        entryPoints: suspend List<ClassDeclaration>.() -> Set<MethodDeclaration> = { publicMethods() }
    ): CallGraph

}


/**
 * Class hierarchy analysis.
 *
 * A virtual or interface call on static receiver type `T` may reach the
 * implementation of the method in `T` or in any of its subtypes, whether or
 * not those subtypes are ever instantiated. Direct calls (static, special and
 * constructor calls) resolve to a single target.
 *
 * The graph is built breadth-first from the entry points. Each level's methods
 * are scanned in parallel on [kotlinx.coroutines.Dispatchers.Default], and the
 * results are merged in order, so the output is deterministic.
 *
 * Limitations: `invokedynamic` call sites (lambdas, method references and so on)
 * produce no edges.
 */
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

    /**
     * Scans one method body and returns the edges its invocations produce,
     * applying [CallGraphSpecification.shouldResolve] and
     * [CallGraphSpecification.shouldInclude]. Runs in parallel with other methods
     * of the same frontier.
     */
    private fun scanMethod(
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

    /**
     * The possible targets of [instruction]: one target for a direct call, and one per
     * distinct implementation across the receiver's subtypes for an indirect call.
     * `invokedynamic` is not resolved.
     */
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


/**
 * Rapid type analysis.
 *
 * Like [CHA], but a virtual or interface call only targets implementations in types
 * that are known to be instantiated, which usually removes many spurious edges.
 * Instantiation is discovered from `new` instructions in the methods reached so far,
 * so the call graph and the set of instantiated types are computed together to a fixed
 * point. When a type becomes instantiated, previously seen call sites on its
 * supertypes receive new edges.
 *
 * To model objects created outside the analyzed code, the types of each entry
 * point's parameters (and all their subtypes) and each entry point's owner are
 * assumed to be instantiated from the start.
 *
 * Methods are scanned in parallel per level, as in [CHA]. Interpreting the scans
 * and updating the graph happens sequentially.
 *
 * Limitations: only `new` instructions count as instantiation (not reflection,
 * deserialization or objects returned by unanalyzed code), and `invokedynamic`
 * call sites produce no edges.
 */
data object RTA : CallGraphType {

    /**
     * What a single method scan found.
     *
     * @property instantiated Types created by `new` in the method.
     * @property directEdges Edges for non-virtual calls, which have exactly one target.
     * @property virtualSites Virtual and interface call sites, resolved later
     * against the types instantiated so far.
     */
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

        fun addEdge(site: CallSite, target: MethodDeclaration) {
            val edge = CallEdge(site, target)
            if (!spec.shouldInclude(edge)) return
            edges += edge
            if (target is ConcreteMethod && spec.shouldExpand(target) && visited.add(target)) {
                next += target
            }
        }

        fun onInstantiated(type: ClassType) {
            if (!instantiated.add(type)) return
            for (owner in hierarchy.supertypes(type, inclusive = true)) {
                for (site in pendingVirtual[owner].orEmpty()) {
                    val call = site.instruction as IndirectInvocationInstruction
                    addEdge(site, hierarchy.resolveMethod(type, call.method))
                }
            }
        }

        fun onVirtualSite(site: CallSite) {
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

    /**
     * Scans one method body: collects instantiated types, resolves direct calls
     * and records virtual call sites for later. Only [CallGraphSpecification.shouldResolve]
     * is applied here. [CallGraphSpecification.shouldInclude] is applied when edges are added.
     */
    private fun scanMethod(hierarchy: ClassHierarchy, method: ConcreteMethod, spec: CallGraphSpecification): RtaScan {
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
