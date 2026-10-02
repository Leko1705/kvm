@file:Suppress("UNUSED")
package com.leko.kvm.bytecode

import com.leko.kvm.ClassFlags
import com.leko.kvm.ConcreteClass
import com.leko.kvm.FieldFlags
import com.leko.kvm.FieldSignature
import com.leko.kvm.MethodFlags
import com.leko.kvm.MethodSignature
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.ReferenceType
import com.leko.kvm.typing.Type
import com.leko.kvm.typing.VoidType
import kotlin.reflect.KProperty


fun buildClass(
    name: String,
    block: ClassBuilderScope.() -> Unit
): ConcreteClass {
    val builder = ClassBuilderImpl(ClassType(name))
    val scope = ClassBuilderScope(builder)
    scope.apply(block)
    return builder.build()
}


fun ClassBuilder.dsl(block: ClassBuilderScope.() -> Unit): ClassBuilder {
    val builder = ClassBuilderScope(this)
    builder.block()
    return this
}

fun MethodBuilder.dsl(block: MethodBuilderScope.() -> Unit): MethodBuilder {
    val builder = MethodBuilderScope(this)
    builder.block()
    return this
}

fun ParametersBuilder.dsl(block: ParametersBuilderScope.() -> Unit): ParametersBuilder {
    val builder = ParametersBuilderScope(this)
    builder.block()
    return this
}

fun Pair<ControlFlowBuilder, ValueBuilder>.dsl(
    block: MethodBodyBuilderScope.() -> Unit
): Pair<ControlFlowBuilder, ValueBuilder> {
    val (flow, values) = this
    val builder = MethodBodyBuilderScope(flow, values)
    builder.block()
    return this
}


@DslMarker
annotation class BytecodeGen

@BytecodeGen
class ClassMemberGeneratorScope internal constructor(
    private val builder: ClassBuilder,
    private var methodFlags: MethodFlags,
    private val annotationApplier: List<ClassBuilderScope.MemberAnnotationPrepare.AnnotationApplier>
) {

    private var type: Type? = null
    private var fieldFlags: FieldFlags = FieldFlags(methodFlags.bits)

    val static: ClassMemberGeneratorScope get() {
        methodFlags += MethodFlags.STATIC
        fieldFlags += FieldFlags.STATIC
        return this
    }

    val final: ClassMemberGeneratorScope get() {
        methodFlags += MethodFlags.FINAL
        fieldFlags += FieldFlags.FINAL
        return this
    }

    val synchronized: ClassMemberGeneratorScope get() {
        methodFlags += MethodFlags.SYNCHRONIZED
        return this
    }

    val bridge: ClassMemberGeneratorScope get() {
        methodFlags += MethodFlags.BRIDGE
        return this
    }

    val varargs: ClassMemberGeneratorScope get() {
        methodFlags += MethodFlags.VARARGS
        return this
    }

    val native: ClassMemberGeneratorScope get() {
        methodFlags += MethodFlags.NATIVE
        return this
    }

    val abstract: ClassMemberGeneratorScope get() {
        methodFlags += MethodFlags.ABSTRACT
        return this
    }

    val synthetic: ClassMemberGeneratorScope get() {
        methodFlags += MethodFlags.SYNTHETIC
        fieldFlags += FieldFlags.SYNTHETIC
        return this
    }

    val volatile: ClassMemberGeneratorScope get() {
        fieldFlags += FieldFlags.VOLATILE
        return this
    }

    val transient: ClassMemberGeneratorScope get() {
        fieldFlags += FieldFlags.TRANSIENT
        return this
    }

    val enum: ClassMemberGeneratorScope get() {
        fieldFlags += FieldFlags.ENUM
        return this
    }

    operator fun get(type: Type): ClassMemberGeneratorScope {
        this.type = type
        return this
    }

    fun method(name: String): MethodBodyPreparer {
        if (fieldFlags hasAny (FieldFlags.FIELD_SPECIFIC + FieldFlags.ENUM)) {
            throw IllegalStateException("can not attach method flags to field")
        }
        if (methodFlags.isStatic && methodFlags.isAbstract) {
            throw IllegalStateException("static method can not be abstract")
        }
        if (methodFlags.isAbstract && methodFlags.isNative) {
            throw IllegalStateException("abstract method can not be native")
        }
        return MethodBodyPreparer(name, annotationApplier)
    }

    fun field(name: String) {
        if (methodFlags hasAny MethodFlags.METHOD_SPECIFIC) {
            throw IllegalStateException("can not attach method flags to field")
        }
        val ty = type ?: throw IllegalStateException("field type is not defined")
        val fb = builder.field(name, ty, fieldFlags)
        annotationApplier.forEach { it.apply(fb) }
    }

    val constructor: MethodBodyPreparer get() {
        if (type == null) type = VoidType
        if (type != VoidType) throw IllegalStateException("Constructors return type must be void")
        if (methodFlags.isStatic) throw IllegalStateException("Constructors must not be static")
        if (methodFlags.isFinal) throw IllegalStateException("Constructors must not be final")
        if (methodFlags.isAbstract) throw IllegalStateException("Constructors must not be abstract")
        if (methodFlags.isNative) throw IllegalStateException("Constructors must not be native")
        return method("<init>")
    }

    inner class MethodBodyPreparer(
        val name: String,
        private val annotationApplier: List<ClassBuilderScope.MemberAnnotationPrepare.AnnotationApplier>
    ) {

        operator fun invoke(vararg params: Pair<String, Type>, block: (MethodBodyBuilderScope.() -> Unit)? = null) {
            val mb = builder.method(name, type ?: VoidType)
            annotationApplier.forEach { it.apply(mb) }
            mb.flags = methodFlags
            val paramBuilder = mb.parameters()
            for ((name, type) in params) {
                paramBuilder.parameter(name, type)
            }
            if (block != null) {
                val (flow, values) = mb.body()
                val scope = MethodBodyBuilderScope(flow, values)
                scope.also(block)
            }
        }

    }

}

@BytecodeGen
class ClassBuilderScope internal constructor(private val builder: ClassBuilder) {

    val PUBLIC      = ClassFlags.PUBLIC
    val FINAL       = ClassFlags.FINAL
    val INTERFACE   = ClassFlags.INTERFACE
    val ABSTRACT    = ClassFlags.ABSTRACT
    val SYNTHETIC   = ClassFlags.SYNTHETIC
    val ANNOTATION  = ClassFlags.ANNOTATION
    val ENUM        = ClassFlags.ENUM

    var flags: ClassFlags
        get() = builder.flags
        set(value) {
            builder.flags = value
        }

    var superClass: ClassType
        get() = builder.superClass
        set(value) {
            builder.superClass = value
        }

    var interfaces: List<ClassType>
        get() = builder.interfaces
        set(value) {
            builder.interfaces = value
        }

    fun annotations(block: AnnotationsBuilderScope.() -> Unit) {
        AnnotationsBuilderScope { builder.annotation(it) }.also(block)
    }

    inner class MemberAnnotationPrepare {

        inner class AnnotationApplier(private val name: String) {
            private val modifiers = mutableListOf<(AnnotationBuilder) -> Unit>()
            fun apply(annotateable: Annotateable) {
                val ab = annotateable.annotation(name)
                modifiers.forEach { it(ab) }
            }
            internal fun put(modifier: (AnnotationBuilder) -> Unit) {
                modifiers.add(modifier)
            }
        }

        private val annotationPrepares = mutableListOf<AnnotationApplier>()

        operator fun get(name: String): ConcreteMemberAnnotationPrepare = ConcreteMemberAnnotationPrepare(name)


        inner class ConcreteMemberAnnotationPrepare(name: String) {
            private val applier = AnnotationApplier(name)

            init {
                annotationPrepares.add(applier)
            }

            val public: ClassMemberGeneratorScope get() = ClassMemberGeneratorScope(builder, MethodFlags.PUBLIC, annotationPrepares)
            val private: ClassMemberGeneratorScope get() = ClassMemberGeneratorScope(builder, MethodFlags.PRIVATE, annotationPrepares)
            val protected: ClassMemberGeneratorScope get() = ClassMemberGeneratorScope(builder, MethodFlags.PROTECTED, annotationPrepares)

            val annotation: MemberAnnotationPrepare get() = this@MemberAnnotationPrepare

            operator fun invoke(vararg args: Pair<String, Any?>): ConcreteMemberAnnotationPrepare {
                applier.put { annoBuilder ->
                    for ((field, value) in args) {
                        when (value) {
                            is Byte -> annoBuilder.put(field, value)
                            is Char -> annoBuilder.put(field, value)
                            is Short -> annoBuilder.put(field, value)
                            is Int -> annoBuilder.put(field, value)
                            is Long -> annoBuilder.put(field, value)
                            is Float -> annoBuilder.put(field, value)
                            is Double -> annoBuilder.put(field, value)
                            is String -> annoBuilder.put(field, value)
                            is Boolean -> annoBuilder.put(field, value)
                            is ClassType -> annoBuilder.put(field, value)
                            is ByteArray -> annoBuilder.put(field, value)
                            is CharArray -> annoBuilder.put(field, value)
                            is ShortArray -> annoBuilder.put(field, value)
                            is IntArray -> annoBuilder.put(field, value)
                            is LongArray -> annoBuilder.put(field, value)
                            is FloatArray -> annoBuilder.put(field, value)
                            is DoubleArray -> annoBuilder.put(field, value)
                            is BooleanArray -> annoBuilder.put(field, value)
                            is Array<*> -> {
                                when {
                                    value.isArrayOf<String>() -> annoBuilder.put(field, value as Array<String>)
                                    value.isArrayOf<ClassType>() -> annoBuilder.put(field, value as Array<ClassType>)
                                    else -> error("Can not store ${value.contentToString()} in annotation")
                                }
                            }

                            else -> error("Can not store $value in annotation")
                        }
                    }
                }
                return this
            }
        }
    }

    val annotation: MemberAnnotationPrepare get() = MemberAnnotationPrepare()

    val public: ClassMemberGeneratorScope get() = ClassMemberGeneratorScope(builder, MethodFlags.PUBLIC, emptyList())

    val private: ClassMemberGeneratorScope get() = ClassMemberGeneratorScope(builder, MethodFlags.PRIVATE, emptyList())

    val protected: ClassMemberGeneratorScope get() = ClassMemberGeneratorScope(builder, MethodFlags.PROTECTED, emptyList())

    fun static(block: MethodBodyBuilderScope.() -> Unit) {
        val (flow, values) = builder.static()
        val scope = MethodBodyBuilderScope(flow, values)
        scope.also(block)
    }

}


@BytecodeGen
class MethodBuilderScope internal constructor(private val builder: MethodBuilder) {

    val PUBLIC       = MethodFlags.PUBLIC
    val PRIVATE      = MethodFlags.PRIVATE
    val PROTECTED    = MethodFlags.PROTECTED
    val STATIC       = MethodFlags.STATIC
    val FINAL        = MethodFlags.FINAL
    val SYNCHRONIZED = MethodFlags.SYNCHRONIZED
    val BRIDGE       = MethodFlags.BRIDGE
    val VARARGS      = MethodFlags.VARARGS
    val NATIVE       = MethodFlags.NATIVE
    val ABSTRACT     = MethodFlags.ABSTRACT
    val SYNTHETIC    = MethodFlags.SYNTHETIC

    var flags: MethodFlags
        get() = builder.flags
        set(value) {
            builder.flags = value
        }

    fun annotations(block: AnnotationsBuilderScope.() -> Unit) {
        AnnotationsBuilderScope { builder.annotation(it) }.also(block)
    }

    fun parameters(block: ParametersBuilderScope.() -> Unit) {
        ParametersBuilderScope(builder.parameters()).also(block)
    }

    fun body(block: MethodBodyBuilderScope.() -> Unit) {
        val (flow, values) = builder.body()
        val scope = MethodBodyBuilderScope(flow, values)
        scope.also(block)
    }
}

@BytecodeGen
class AnnotationsBuilderScope internal constructor(private val builder: (String) -> AnnotationBuilder) {
    fun annotation(name: String, block: (AnnotationBuilderScope.() -> Unit)? = null) {
        val builder = AnnotationBuilderScope(builder(name))
        if (block != null) {
            builder.block()
        }
    }
}

@BytecodeGen
class AnnotationBuilderScope internal constructor(private val builder: AnnotationBuilder) {

    inner class Fields {
        operator fun set(field: String, value: Byte) = builder.put(field, value)
        operator fun set(field: String, value: Char) = builder.put(field, value)
        operator fun set(field: String, value: Short) = builder.put(field, value)
        operator fun set(field: String, value: Int) = builder.put(field, value)
        operator fun set(field: String, value: Long) = builder.put(field, value)
        operator fun set(field: String, value: Float) = builder.put(field, value)
        operator fun set(field: String, value: Double) = builder.put(field, value)
        operator fun set(field: String, value: Boolean) = builder.put(field, value)
        operator fun set(field: String, value: String) = builder.put(field, value)
        operator fun set(field: String, value: ClassType) = builder.put(field, value)
        operator fun set(field: String, value: ByteArray) = builder.put(field, value)
        operator fun set(field: String, value: CharArray) = builder.put(field, value)
        operator fun set(field: String, value: ShortArray) = builder.put(field, value)
        operator fun set(field: String, value: IntArray) = builder.put(field, value)
        operator fun set(field: String, value: LongArray) = builder.put(field, value)
        operator fun set(field: String, value: FloatArray) = builder.put(field, value)
        operator fun set(field: String, value: DoubleArray) = builder.put(field, value)
        operator fun set(field: String, value: BooleanArray) = builder.put(field, value)
        operator fun set(field: String, value: Array<String>) = builder.put(field, value)
        operator fun set(field: String, value: Array<ClassType>) = builder.put(field, value)
    }

    val fields = Fields()

}

@BytecodeGen
class ParametersBuilderScope internal constructor(private val builder: ParametersBuilder) {
    fun parameter(name: String, type: Type) {
        builder.parameter(name, type)
    }
}


@BytecodeGen
class MethodBodyBuilderScope(
    private val flow: ControlFlowBuilder,
    private val values: ValueBuilder
) {

    fun parameter(name: String): LocalPtr = values.parameter(name)

    fun local(type: Type): LocalPtr = values.local(type)

    fun Label(): Label = values.label()

    val THIS: Value get() = values.THIS

    fun line(n: Int) {
        flow.line(n)
    }

    fun placeLabel(label: Label) {
        flow.placeLabel(label)
    }

    fun goto(label: Label) {
        flow.goto(label)
    }

    fun Value.eval(pop: Boolean = true) {
        flow.eval(this, pop)
    }

    fun returns(value: Value? = null) {
        flow.returns(value)
    }

    operator fun LocalPtr.setValue(receiver: Nothing?, property: KProperty<*>, value: Value) {
        store(this, value)
    }

    operator fun Value.set(index: Value, value: Value) {
        store(value[index], value)
    }

    fun FieldPtr.set(value: Value) {
        store(this, value)
    }

    fun store(ptr: Ptr, value: Value) {
        flow.store(ptr, value)
    }

    inner class Loop(
        internal val head: Label = values.label(),
        internal var tail: Label? = null,
    )

    fun loop(body: MethodBodyBuilderScope.(Loop) -> Unit) {
        val loop = Loop()
        flow.placeLabel(loop.head)
        body(loop)
        flow.goto(loop.head)

        val tail = loop.tail
        if (tail != null) {
            flow.placeLabel(tail)
        }
    }

    fun whileLoop(condition: Value, body: MethodBodyBuilderScope.(Loop) -> Unit) {
        loop { loop ->
            ifTrue(!condition) {
                escape(loop)
            }.eval()
            body(loop)
        }
    }

    fun whileLoop(condition: Boolean, body: MethodBodyBuilderScope.(Loop) -> Unit) =
        whileLoop(bool(condition), body)

    fun escape(loop: Loop) {
        var tail = loop.tail
        if (tail == null) tail = values.label()
        loop.tail = tail
        goto(tail)
    }

    fun next(loop: Loop) {
        goto(loop.head)
    }

    fun ifTrue(condition: Value, body: MethodBodyBuilderScope.() -> Unit): BranchExtensionBuilder {
        val thenFlow = flow.branch(condition)
        val scope = MethodBodyBuilderScope(thenFlow, values)
        scope.body()
        val branchCompleter = thenFlow.close()
        return BranchExtensionBuilder(branchCompleter, values)
    }

    fun ifTrue(condition: Boolean, body: MethodBodyBuilderScope.() -> Unit): BranchExtensionBuilder  = ifTrue(bool(condition), body)

    fun switch(value: Value, block: SwitchBuilderScope.() -> Unit) {
        val switchBuilder = flow.switch(value)
        val scope = SwitchBuilderScope( switchBuilder, values)
        scope.block()
    }

    fun attempt(block: MethodBodyBuilderScope.() -> Unit): AttemptExtensionBuilder {
        val attemptFlow = flow.attempt()
        val scope = MethodBodyBuilderScope(attemptFlow, values)
        scope.block()
        val attemptCompleter = attemptFlow.close()
        return AttemptExtensionBuilder(attemptCompleter, values)
    }

    fun superCall(vararg arguments: Value) {
        flow.superCall(*arguments)
    }

    fun asm(block: AsmGeneratorScope.() -> Unit) {
        AsmGeneratorScope(flow).also(block)
    }
}


@BytecodeGen
class BranchExtensionBuilder internal constructor(
    private val branchCompleter: BranchCompleter,
    private val values: ValueBuilder
) {
    fun otherwise(body: MethodBodyBuilderScope.() -> Unit) {
        val flow = branchCompleter.otherwise()
        val scope = MethodBodyBuilderScope(flow, values)
        scope.body()
        flow.close()
    }

    fun eval() {
        branchCompleter.complete()
    }
}


@BytecodeGen
class SwitchBuilderScope internal constructor(
    private val builder: SwitchBuilder,
    private val values: ValueBuilder
) {

    fun case(key: Int, body: MethodBodyBuilderScope.() -> Unit) {
        val caseFlow = builder.case(key)
        val scope = MethodBodyBuilderScope(caseFlow, values)
        scope.body()
        caseFlow.close()
    }

    fun otherwise(body: MethodBodyBuilderScope.() -> Unit) {
        val otherwiseScope = builder.otherwise()
        val scope = MethodBodyBuilderScope(otherwiseScope, values)
        scope.body()
        otherwiseScope.close()
    }

}


@BytecodeGen
class AttemptExtensionBuilder internal constructor(
    private val attemptCompleter: AttemptCompleter,
    private val values: ValueBuilder
) {

    fun rescue(type: ClassType, body: MethodBodyBuilderScope.() -> Unit): AttemptExtensionBuilder {
        val flow = attemptCompleter.rescue(type)
        val scope = MethodBodyBuilderScope(flow, values)
        scope.body()
        flow.close()
        return this
    }

    fun finally(body: MethodBodyBuilderScope.() -> Unit) {
        val flow = attemptCompleter.finally()
        val scope = MethodBodyBuilderScope(flow, values)
        scope.body()
        flow.close()
    }
}

@BytecodeGen
class AsmGeneratorScope internal constructor(private val flow: ControlFlowBuilder) {

    val ACONST_NULL: Unit get() {
        flow.instruction(AConstNullInstruction)
    }

    val NOP: Unit get() {
        flow.instruction(NopInstruction)
    }

    val ICONST_M1: Unit get() {
        flow.instruction(IConstM1Instruction)
    }

    val ICONST_0: Unit get() {
        flow.instruction(IConst0Instruction)
    }

    val ICONST_1: Unit get() {
        flow.instruction(IConst1Instruction)
    }

    val ICONST_2: Unit get() {
        flow.instruction(IConst2Instruction)
    }

    val ICONST_3: Unit get() {
        flow.instruction(IConst3Instruction)
    }

    val ICONST_4: Unit get() {
        flow.instruction(IConst4Instruction)
    }

    val ICONST_5: Unit get() {
        flow.instruction(IConst5Instruction)
    }

    val LCONST_0: Unit get() {
        flow.instruction(LConst0Instruction)
    }

    val LCONST_1: Unit get() {
        flow.instruction(LConst0Instruction)
    }

    val FCONST_0: Unit get() {
        flow.instruction(FConst0Instruction)
    }

    val FCONST_1: Unit get() {
        flow.instruction(FConst1Instruction)
    }

    val FCONST_2: Unit get() {
        flow.instruction(FConst2Instruction)
    }

    val DCONST_0: Unit get() {
        flow.instruction(DConst0Instruction)
    }

    val DCONST_1: Unit get() {
        flow.instruction(DConst1Instruction)
    }

    val BIPUSH: (Byte) -> Unit get() = {
        flow.instruction(BipushInstruction(it))
    }

    val SIPUSH: (Short) -> Unit get() = {
        flow.instruction(SipushInstruction(it))
    }

    val LDC: (Any) -> Unit get() = {
        flow.instruction(LdcInstruction(it))
    }

    val ILOAD: (Int) -> Unit get() = {
        flow.instruction(ILoadInstruction(it))
    }

    val LLOAD: (Int) -> Unit get() = {
        flow.instruction(LLoadInstruction(it))
    }

    val FLOAD: (Int) -> Unit get() = {
        flow.instruction(FLoadInstruction(it))
    }

    val DLOAD: (Int) -> Unit get() = {
        flow.instruction(DLoadInstruction(it))
    }

    val ALOAD: (Int) -> Unit get() = {
        flow.instruction(ALoadInstruction(it))
    }

    val IALOAD: Unit get() {
        flow.instruction(IALoadInstruction)
    }

    val LALOAD: Unit get() {
        flow.instruction(LALoadInstruction)
    }

    val FALOAD: Unit get() {
        flow.instruction(FALoadInstruction)
    }

    val DALOAD: Unit get() {
        flow.instruction(DALoadInstruction)
    }

    val AALOAD: Unit get() {
        flow.instruction(AALoadInstruction)
    }

    val BALOAD: Unit get() {
        flow.instruction(BALoadInstruction)
    }

    val CALOAD: Unit get() {
        flow.instruction(CALoadInstruction)
    }

    val SALOAD: Unit get() {
        flow.instruction(SALoadInstruction)
    }

    val ISTORE: (Int) -> Unit get() = {
        flow.instruction(IStoreInstruction(it))
    }

    val LSTORE: (Int) -> Unit get() = {
        flow.instruction(LStoreInstruction(it))
    }

    val FSTORE: (Int) -> Unit get() = {
        flow.instruction(FStoreInstruction(it))
    }

    val DSTORE: (Int) -> Unit get() = {
        flow.instruction(DStoreInstruction(it))
    }

    val ASTORE: (Int) -> Unit get() = {
        flow.instruction(AStoreInstruction(it))
    }

    val IASTORE: Unit get() {
        flow.instruction(IAStoreInstruction)
    }

    val LASTORE: Unit get() {
        flow.instruction(LAStoreInstruction)
    }

    val FASTORE: Unit get() {
        flow.instruction(FAStoreInstruction)
    }

    val DASTORE: Unit get() {
        flow.instruction(DAStoreInstruction)
    }

    val AASTORE: Unit get() {
        flow.instruction(AAStoreInstruction)
    }

    val BASTORE: Unit get() {
        flow.instruction(BAStoreInstruction)
    }

    val CASTORE: Unit get() {
        flow.instruction(CAStoreInstruction)
    }

    val SASTORE: Unit get() {
        flow.instruction(SAStoreInstruction)
    }

    val POP: Unit get() {
        flow.instruction(PopInstruction)
    }

    val POP2: Unit get() {
        flow.instruction(Pop2Instruction)
    }

    val DUP: Unit get() {
        flow.instruction(DupInstruction)
    }

    val DUP_X1: Unit get() {
        flow.instruction(DupX1Instruction)
    }

    val DUP_X2: Unit get() {
        flow.instruction(DupX2Instruction)
    }

    val DUP2: Unit get() {
        flow.instruction(Dup2Instruction)
    }

    val DUP2_X1: Unit get() {
        flow.instruction(Dup2X1Instruction)
    }

    val DUP2_X2: Unit get() {
        flow.instruction(Dup2X2Instruction)
    }

    val SWAP: Unit get() {
        flow.instruction(SwapInstruction)
    }

    val IADD: Unit get() {
        flow.instruction(IAddInstruction)
    }

    val LADD: Unit get() {
        flow.instruction(LAddInstruction)
    }

    val FADD: Unit get() {
        flow.instruction(FAddInstruction)
    }

    val DADD: Unit get() {
        flow.instruction(DAddInstruction)
    }

    val ISUB: Unit get() {
        flow.instruction(ISubInstruction)
    }

    val LSUB: Unit get() {
        flow.instruction(LSubInstruction)
    }

    val FSUB: Unit get() {
        flow.instruction(FSubInstruction)
    }

    val DSUB: Unit get() {
        flow.instruction(DSubInstruction)
    }

    val IMUL: Unit get() {
        flow.instruction(IMulInstruction)
    }

    val LMUL: Unit get() {
        flow.instruction(LMulInstruction)
    }

    val FMUL: Unit get() {
        flow.instruction(FMulInstruction)
    }

    val DMUL: Unit get() {
        flow.instruction(DMulInstruction)
    }

    val IDIV: Unit get() {
        flow.instruction(IDivInstruction)
    }

    val LDIV: Unit get() {
        flow.instruction(LDivInstruction)
    }

    val FDIV: Unit get() {
        flow.instruction(FDivInstruction)
    }

    val DDIV: Unit get() {
        flow.instruction(DDivInstruction)
    }

    val IREM: Unit get() {
        flow.instruction(IRemInstruction)
    }

    val LREM: Unit get() {
        flow.instruction(LRemInstruction)
    }

    val FREM: Unit get() {
        flow.instruction(FReturnInstruction)
    }

    val DREM: Unit get() {
        flow.instruction(DReturnInstruction)
    }

    val INEG: Unit get() {
        flow.instruction(INegInstruction)
    }

    val LNEG: Unit get() {
        flow.instruction(LNegInstruction)
    }

    val FNEG: Unit get() {
        flow.instruction(FNegInstruction)
    }

    val DNEG: Unit get() {
        flow.instruction(DNegInstruction)
    }

    val ISHL: Unit get() {
        flow.instruction(IShlInstruction)
    }

    val LSHL: Unit get() {
        flow.instruction(LShlInstruction)
    }

    val ISHR: Unit get() {
        flow.instruction(IShrInstruction)
    }

    val LSHR: Unit get() {
        flow.instruction(LShrInstruction)
    }

    val IUSHR: Unit get() {
        flow.instruction(IUshrInstruction)
    }

    val LUSHR: Unit get() {
        flow.instruction(LUshrInstruction)
    }

    val IAND: Unit get() {
        flow.instruction(IAndInstruction)
    }

    val LAND: Unit get() {
        flow.instruction(LAndInstruction)
    }

    val IOR: Unit get() {
        flow.instruction(IOrInstruction)
    }

    val LOR: Unit get() {
        flow.instruction(LOrInstruction)
    }

    val IXOR: Unit get() {
        flow.instruction(IXorInstruction)
    }

    val LXOR: Unit get() {
        flow.instruction(LXorInstruction)
    }

    val IINC: (Int, Int) -> Unit get() = { idx, amount ->
        flow.instruction(IIncInstruction(idx, amount))
    }

    val I2L: Unit get() {
        flow.instruction(I2LInstruction)
    }

    val I2F: Unit get() {
        flow.instruction(I2FInstruction)
    }

    val I2D: Unit get() {
        flow.instruction(I2DInstruction)
    }

    val L2I: Unit get() {
        flow.instruction(L2IInstruction)
    }

    val L2F: Unit get() {
        flow.instruction(L2FInstruction)
    }

    val L2D: Unit get() {
        flow.instruction(L2DInstruction)
    }

    val F2I: Unit get() {
        flow.instruction(F2IInstruction)
    }

    val F2L: Unit get() {
        flow.instruction(F2LInstruction)
    }

    val F2D: Unit get() {
        flow.instruction(F2DInstruction)
    }

    val D2I: Unit get() {
        flow.instruction(D2IInstruction)
    }

    val D2L: Unit get() {
        flow.instruction(D2LInstruction)
    }

    val D2F: Unit get() {
        flow.instruction(D2FInstruction)
    }

    val I2B: Unit get() {
        flow.instruction(I2BInstruction)
    }

    val I2C: Unit get() {
        flow.instruction(I2CInstruction)
    }

    val I2S: Unit get() {
        flow.instruction(I2SInstruction)
    }

    val LCMP: Unit get() {
        flow.instruction(LcmpInstruction)
    }

    val FCMPL: Unit get() {
        flow.instruction(FcmplInstruction)
    }

    val FCMPG: Unit get() {
        flow.instruction(FcmpgInstruction)
    }

    val DCMPL: Unit get() {
        flow.instruction(DcmplInstruction)
    }

    val DCMPG: Unit get() {
        flow.instruction(DcmpgInstruction)
    }

    val IFEQ: (Label) -> Unit get() = {
        flow.instruction(IfEqInstruction(it))
    }

    val IFNE: (Label) -> Unit get() = {
        flow.instruction(IfNeInstruction(it))
    }

    val IFLT: (Label) -> Unit get() = {
        flow.instruction(IfLtInstruction(it))
    }

    val IFGE: (Label) -> Unit get() = {
        flow.instruction(IfGeInstruction(it))
    }

    val IFGT: (Label) -> Unit get() = {
        flow.instruction(IfGtInstruction(it))
    }

    val IFLE: (Label) -> Unit get() = {
        flow.instruction(IfLeInstruction(it))
    }

    val IF_ICMPEQ: (Label) -> Unit get() = {
        flow.instruction(IfICmpEqInstruction(it))
    }

    val IF_ICMPNE: (Label) -> Unit get() = {
        flow.instruction(IfICmpNeInstruction(it))
    }

    val IF_ICMPLT: (Label) -> Unit get() = {
        flow.instruction(IfICmpLtInstruction(it))
    }

    val IF_ICMPGE: (Label) -> Unit get() = {
        flow.instruction(IfICmpGeInstruction(it))
    }

    val IF_ICMPGT: (Label) -> Unit get() = {
        flow.instruction(IfICmpGtInstruction(it))
    }

    val IF_ICMPLE: (Label) -> Unit get() = {
        flow.instruction(IfICmpLeInstruction(it))
    }

    val IF_ACMPEQ: (Label) -> Unit get() = {
        flow.instruction(IfACmpEqInstruction(it))
    }

    val IF_ACMPNE: (Label) -> Unit get() = {
        flow.instruction(IfACmpNeInstruction(it))
    }

    val GOTO: (Label) -> Unit get() = {
        flow.instruction(GotoInstruction(it))
    }

    val JSR: (Label) -> Unit get() = {
        flow.instruction(JsrInstruction)
    }

    val RET: (Int) -> Unit get() = {
        flow.instruction(RetInstruction)
    }

    val TABLESWITCH: (Label, Map<Int, Label>) -> Unit get() = { label, table ->
        flow.instruction(TableSwitchInstruction(label, table))
    }

    val LOOKUPSWITCH: (Label, Map<Int, Label>) -> Unit get() = { label, table ->
        flow.instruction(LookupSwitchInstruction(label, table))
    }

    val IRETURN: Unit get() {
        flow.instruction(IReturnInstruction)
    }

    val LRETURN: Unit get() {
        flow.instruction(LReturnInstruction)
    }

    val FRETURN: Unit get() {
        flow.instruction(FReturnInstruction)
    }

    val DRETURN: Unit get() {
        flow.instruction(DReturnInstruction)
    }

    val ARETURN: Unit get() {
        flow.instruction(AReturnInstruction)
    }

    val RETURN: Unit get() {
        flow.instruction(VReturnInstruction)
    }

    val GETSTATIC: (FieldSignature) -> Unit get() = {
        flow.instruction(GetStaticInstruction(it))
    }

    val PUTSTATIC: (FieldSignature) -> Unit get() = {
        flow.instruction(PutStaticInstruction(it))
    }

    val GETFIELD: (FieldSignature) -> Unit get() = {
        flow.instruction(GetFieldInstruction(it))
    }

    val PUTFIELD: (FieldSignature) -> Unit get() = {
        flow.instruction(PutFieldInstruction(it))
    }

    val INVOKEVIRTUAL: (ClassType, MethodSignature) -> Unit get() = { t, s ->
        flow.instruction(InvokeVirtualInstruction(t, s))
    }

    val INVOKESPECIAL: (ClassType, MethodSignature) -> Unit get() = { t, s ->
        flow.instruction(InvokeSpecialInstruction(t, s))
    }

    val INVOKESTATIC: (ClassType, MethodSignature) -> Unit get() = { t, s ->
        flow.instruction(InvokeStaticInstruction(t, s))
    }

    val INVOKEINTERFACE: (ClassType, MethodSignature) -> Unit get() = { t, s ->
        flow.instruction(InvokeInterfaceInstruction(t, s))
    }

    val INVOKEDYNAMIC: (MethodSignature, MethodHandle, List<BootstrapArgument>) -> Unit get() = { m, h, b ->
        flow.instruction(InvokeDynamicInstruction(m, h, b))
    }

    val NEW: (ClassType) -> Unit get() = {
        flow.instruction(NewInstruction(it))
    }

    val NEWARRAY: (Type, Int) -> Unit get() = { type, dim ->
        flow.instruction(NewArrayInstruction(type, dim))
    }

    val ANEWARRAY: (Type, Int) -> Unit get() = { type, dim ->
        flow.instruction(ANewArrayInstruction(type, dim))
    }

    val ARRAYLENGTH: Unit get() {
        flow.instruction(ArrayLengthInstruction)
    }

    val ATHROW: Unit get() {
        flow.instruction(AThrowInstruction)
    }

    val CHECKCAST: (ReferenceType) -> Unit get() = {
        flow.instruction(CheckCastInstruction(it))
    }

    val INSTANCEOF: (ReferenceType) -> Unit get() = {
        flow.instruction(InstanceOfInstruction(it))
    }

    val MONITORENTER: Unit get() {
        flow.instruction(MonitorEnterInstruction)
    }

    val MONITOREXIT: Unit get() {
        flow.instruction(MonitorExitInstruction)
    }

    val MULTIANEWARRAY: (Type, Int) -> Unit get() = { type, dim ->
        flow.instruction(MultiANewArrayInstruction(type, dim))
    }

    val IFNULL: (Label) -> Unit get() = {
        flow.instruction(IfNullInstruction(it))
    }

    val IFNONNULL: (Label) -> Unit get() = {
        flow.instruction(IfNonNullInstruction(it))
    }
}
