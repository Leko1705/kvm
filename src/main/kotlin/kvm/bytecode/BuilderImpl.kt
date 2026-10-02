package com.leko.kvm.bytecode

import com.leko.kvm.AbstractMethod
import com.leko.kvm.ClassFlags
import com.leko.kvm.ConcreteClass
import com.leko.kvm.ConcreteField
import com.leko.kvm.ConcreteMethod
import com.leko.kvm.FieldFlags
import com.leko.kvm.FieldSignature
import com.leko.kvm.KvmAnnotation
import com.leko.kvm.MethodDescriptor
import com.leko.kvm.MethodFlags
import com.leko.kvm.MethodSignature
import com.leko.kvm.PresentMethodDeclaration
import com.leko.kvm.name
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.Type
import com.leko.kvm.typing.VoidType
import kotlin.collections.set


class AnnotationBuilderImpl(name: String): CompletableAnnotationBuilder {
    
    private val fields = mutableMapOf<String, Any>()
    
    private var annotation = KvmAnnotation(name, fields)
    
    override fun put(field: String, value: Byte): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: Char): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: Short): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: Int): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: Long): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: Float): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: Double): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: String): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: Boolean): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: ClassType): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: ByteArray): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: CharArray): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: ShortArray): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: IntArray): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: LongArray): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: FloatArray): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: DoubleArray): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(
        field: String,
        value: Array<String>
    ): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(field: String, value: BooleanArray): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun put(
        field: String,
        value: Array<ClassType>
    ): AnnotationBuilder {
        fields[field] = value
        return this
    }

    override fun build(): KvmAnnotation = annotation

}

class ClassBuilderImpl internal constructor(private val thisType: ClassType): CompletableClassBuilder {

    private val fields = mutableListOf<ConcreteField>()

    private val methods = mutableListOf<PresentMethodDeclaration>()

    private val annotations = mutableListOf<KvmAnnotation>()

    private val todos = mutableListOf<Runnable>()


    override var flags: ClassFlags = ClassFlags.EMPTY

    override var superClass: ClassType = ClassType("java.lang.Object")

    override var interfaces = emptyList<ClassType>()

    override fun annotation(name: String): AnnotationBuilder {
        val builder = AnnotationBuilderImpl(name)
        val anno = builder.build()
        annotations.add(anno)
        return builder
    }

    override fun method(name: String, returnType: Type): CompletableMethodBuilder {
        val mb = MethodBuilderImpl(name, thisType, returnType, superClass, flags.isAbstract)
        todos.add {
            val method = mb.build()
            methods.add(method)
        }
        return mb
    }

    override fun constructor(): MethodBuilder = method("<init>", VoidType)

    override fun static(): Pair<ControlFlowBuilder, ValueBuilder> {
        return method("<clinit>", VoidType).body()
    }

    override fun field(name: String, type: Type, accessFlags: FieldFlags): FieldBuilder {
        return object : CompletableFieldBuilder {
            private val annotations = mutableListOf<KvmAnnotation>()

            override fun annotation(name: String): AnnotationBuilder {
                val builder = AnnotationBuilderImpl(name)
                val anno = builder.build()
                annotations.add(anno)
                return builder
            }

            override fun build(): ConcreteField {
                val fieldDecl = ConcreteField(
                    FieldSignature(
                        thisType,
                        name,
                        type
                    ),
                    accessFlags,
                    annotations
                )
                fields.add(fieldDecl)
                return fieldDecl
            }
        }
    }

    override fun build(): ConcreteClass {
        todos.forEach { it.run() }
        if (!methods.any { it.name == "<init>" }) {
            throw IllegalStateException("no constructor defined")
        }
        return ConcreteClass(
            thisType,
            superClass,
            interfaces,
            annotations,
            methods,
            fields,
            flags,
        )
    }
}


class MethodBuilderImpl internal constructor(
    private val name: String,
    private val thisType: ClassType,
    var returnType: Type,
    var superClass: ClassType,
    private val isAbstract: Boolean,
): CompletableMethodBuilder {

    private val parameters = mutableMapOf<String, LocalPtr>()
    private var bodyBuilder: MethodBodyBuilderImpl? = null
    private val annotations = mutableListOf<KvmAnnotation>()

    override var flags: MethodFlags = MethodFlags.EMPTY

    override fun annotation(name: String): AnnotationBuilder {
        val builder = AnnotationBuilderImpl(name)
        val anno = builder.build()
        annotations.add(anno)
        return builder
    }

    override fun parameters(): ParametersBuilder = ParametersBuilderImpl(parameters)

    override fun body(): Pair<ControlFlowBuilder, ValueBuilder> {
        val bodyBuilder = MethodBodyBuilderImpl(name, parameters, thisType, superClass, flags.isStatic)
        this.bodyBuilder = bodyBuilder
        return bodyBuilder to bodyBuilder
    }

    override fun build(): PresentMethodDeclaration {

        val signature = MethodSignature(
            name,
            MethodDescriptor(
                parameters.values.map { it.type },
                returnType
            )
        )

        val builder = this.bodyBuilder
        return if (builder != null) {
            if (flags.isNative) throw IllegalStateException("Native method must not have a body")
            val body = Sequence(builder.statements)
            ConcreteMethod(thisType, signature, flags, annotations, body.toMethodBody())
        }
        else {
            if (!isAbstract && !flags.isNative)
                throw IllegalStateException("Missing method body for $name")
            AbstractMethod(thisType, signature, flags, annotations)
        }
    }

}

class ParametersBuilderImpl internal constructor(
    private val parameters: MutableMap<String, LocalPtr>
): ParametersBuilder {

    override fun parameter(name: String, type: Type): ParametersBuilder {
        if (name in parameters) {
            throw IllegalArgumentException("Parameter $name is already defined")
        }
        parameters[name] = LocalPtr(parameters.size, type)
        return this
    }

}


class MethodBodyBuilderImpl internal constructor(
    private val methodName: String,
    internal val parameters: MutableMap<String, LocalPtr>,
    private val thisType: ClassType,
    private val superClass: ClassType,
    private val isStatic: Boolean,
): ControlFlowBuilder, ValueBuilder {

    internal var statements = mutableListOf<Statement>()

    private var nextLocal = if (isStatic) 0 else 1

    private var nextLabelId = 0

    private fun Label(): Label = Label((nextLabelId++).toString())

    override val THIS: Value get() {
        if (isStatic) throw IllegalStateException("This cannot be used from a static methods")
        return LocalPtr(0, thisType)
    }

    override fun label(): Label = Label()

    override fun parameter(name: String): LocalPtr = parameters[name] ?: throw IllegalArgumentException("Parameter $name not found")

    override fun local(type: Type): LocalPtr = LocalPtr(nextLocal++, type)

    override fun line(n: Int): ControlFlowBuilder {
        statements.add(LineNumber(n))
        return this
    }

    override fun placeLabel(label: Label): ControlFlowBuilder {
        statements.add(LabelInst(label))
        return this
    }

    override fun goto(label: Label): ControlFlowBuilder {
        statements.add(Goto(label))
        return this
    }

    override fun branch(
        condition: Value
    ): ScopedControlFlowBuilder<BranchCompleter> {

        val endLabel = Label()
        val falseLabel = Label()

        val branch = Branch(condition) { IfEqInstruction(falseLabel) }
        statements.add(branch)

        return object : ScopedControlFlowBuilder<BranchCompleter>(this) {
            override fun close(): BranchCompleter {

                return object : BranchCompleter {

                    override fun otherwise(): ScopedControlFlowBuilder<ControlFlowBuilder> {
                        statements.add(Goto(endLabel))
                        placeLabel(falseLabel)

                        return object : ScopedControlFlowBuilder<ControlFlowBuilder>(this@MethodBodyBuilderImpl) {
                            override fun close(): ControlFlowBuilder {
                                placeLabel(endLabel)
                                return this@MethodBodyBuilderImpl
                            }
                        }
                    }

                    override fun complete(): ControlFlowBuilder {
                        placeLabel(falseLabel)
                        return this@MethodBodyBuilderImpl
                    }
                }

            }
        }


    }

    override fun eval(value: Value, pop: Boolean): ControlFlowBuilder {
        if (pop) statements.add(Pop(value))
        else statements.add(Pop(value))
        return this
    }


    override fun switch(value: Value): SwitchBuilder {

        val cases = mutableMapOf<Int, Label>()
        val endLabel = Label()
        val defaultLabel = Label()
        val dispatchPlaceholder = SwitchDispatch(value, cases, defaultLabel)

        statements.add(dispatchPlaceholder)

        class Sb: SwitchBuilder {

            override fun case(key: Int): ScopedControlFlowBuilder<SwitchBuilder> {
                val caseLabel = Label()
                cases[key] = caseLabel
                statements.add(LabelInst(caseLabel))
                return object : ScopedControlFlowBuilder<SwitchBuilder>(this@MethodBodyBuilderImpl) {
                    override fun close(): SwitchBuilder {
                        statements.add(Goto(endLabel))
                        return this@Sb
                    }
                }
            }

            override fun otherwise(): ScopedControlFlowBuilder<ControlFlowBuilder> {
                statements.add(LabelInst(defaultLabel))
                return object : ScopedControlFlowBuilder<ControlFlowBuilder>(this@MethodBodyBuilderImpl) {
                    override fun close(): ControlFlowBuilder = complete()
                }
            }

            override fun complete(): ControlFlowBuilder {
                statements.add(LabelInst(endLabel))
                return this@MethodBodyBuilderImpl
            }
        }

        return Sb()
    }

    override fun attempt(): ScopedControlFlowBuilder<AttemptCompleter> {
        val tryStart = Label()
        val tryEnd = Label()
        val endLabel = Label()
        statements.add(LabelInst(tryStart))

        return object : ScopedControlFlowBuilder<AttemptCompleter>(this@MethodBodyBuilderImpl) {

            override fun close(): AttemptCompleter {
                statements.add(Goto(endLabel))
                statements.add(LabelInst(tryEnd))
                val endLabelInst = LabelInst(endLabel)
                statements.add(endLabelInst)

                class Ac: AttemptCompleter  {

                    override fun rescue(type: ClassType): ScopedControlFlowBuilder<AttemptCompleter> {
                        val handlerLabel = Label()
                        statements.remove(endLabelInst)
                        statements.add(ExceptionHandlerEntry(tryStart, tryEnd, handlerLabel, type))
                        statements.add(LabelInst(handlerLabel))

                        // TODO store caught exception in local register HERE

                        return object : ScopedControlFlowBuilder<AttemptCompleter>(this@MethodBodyBuilderImpl) {
                            override fun close(): AttemptCompleter {
                                statements.add(Goto(endLabelInst.label))
                                statements.add(endLabelInst)
                                return this@Ac
                            }
                        }
                    }

                    override fun finally(): ScopedControlFlowBuilder<ControlFlowBuilder> {
                        val handlerLabel = Label()
                        statements.remove(endLabelInst)
                        statements.add(ExceptionHandlerEntry(tryStart, tryEnd, handlerLabel, null))
                        statements.add(LabelInst(handlerLabel))

                        return object : ScopedControlFlowBuilder<ControlFlowBuilder>(this@MethodBodyBuilderImpl) {
                            override fun close(): ControlFlowBuilder {
                                statements.add(endLabelInst)
                                return complete()
                            }
                        }

                    }

                    override fun complete(): ControlFlowBuilder {
                        return this@MethodBodyBuilderImpl
                    }

                }

                return Ac()
            }

        }
    }


    override fun returns(value: Value?): ControlFlowBuilder {
        statements.add(Return(value))
        return this
    }

    override fun throws(value: Value): ControlFlowBuilder {
        statements.add(Throw(value))
        return this
    }

    override fun store(ptr: Ptr, value: Value): ControlFlowBuilder {
        statements.add(Store(ptr, value))
        return this
    }

    override fun superCall(vararg arguments: Value): ControlFlowBuilder {
        if (methodName != "<init>") {
            throw IllegalStateException("superCall() can only be used inside a constructor")
        }
        val realStatements = statements.filterNot { it is LineNumber }
        if (realStatements.isNotEmpty()) {
            throw IllegalStateException("superCall() must be the first statement of the constructor")
        }
        statements.add(Eval(CallSpecial(THIS, superClass, "<init>", arguments.toList(), VoidType)))
        return this
    }

    override fun instruction(instruction: Instruction): ControlFlowBuilder {
        statements.add(Inst(instruction))
        return this
    }

}
