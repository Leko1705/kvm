package com.leko.kvm.bytecode

import com.leko.kvm.*
import com.leko.kvm.typing.*
import com.leko.kvm.typing.Type
import org.objectweb.asm.*
import org.objectweb.asm.Label as AsmLabel
import org.objectweb.asm.Type as AsmType


/**
 * Default bytecode generator and parser for the in-memory representation used by
 * this package.
 *
 * The implementation translates [ConcreteClass] instances into JVM bytecode via
 * ASM and can also read selected bytecode structures back into the intermediate
 * representation.
 */
object ASM : BytecodeGenerator, BytecodeParser {

    override fun generate(clazz: ConcreteClass): ByteArray {
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)

        writer.visit(
            Opcodes.V11,
            clazz.accessFlags.bits,
            clazz.type.jvmName,                          // e.g. "com/example/MyClass"
            null,                                         // signature (generics) — null for now
            clazz.superClass?.jvmName ?: "java/lang/Object",
            clazz.interfaces.map { it.jvmName }.toTypedArray()
        )

        writeAnnotations(clazz.annotations) { desc, visible -> writer.visitAnnotation(desc, visible) }

        // fields
        clazz.fields.forEach { field ->
            val fv = writer.visitField(
                field.accessFlags.bits,
                field.signature.name,
                field.signature.type.jvmName,
                null,   // generic signature — null for now
                null,   // constant value — null for now
            )
            writeAnnotations(field.annotations) { desc, visible -> fv.visitAnnotation(desc, visible) }
            fv.visitEnd()
        }

        // methods
        clazz.methods.forEach { method ->
            val descriptor = buildMethodDescriptor(method.signature)
            val mv = writer.visitMethod(
                method.accessFlags.bits,
                method.name,
                descriptor,
                null,   // generic signature — null for now
                null,   // exceptions — null for now
            )
            writeAnnotations(method.annotations) { desc, visible -> mv.visitAnnotation(desc, visible) }
            mv.visitCode()
            when (method) {
                is ConcreteMethod -> generate(method.body, mv)
                is AbstractMethod -> { /* no body */
                }
            }
            mv.visitMaxs(0, 0)  // computed by COMPUTE_FRAMES
            mv.visitEnd()
        }

        writer.visitEnd()
        return writer.toByteArray()
    }

    private fun dottedToDescriptor(dottedName: String): String =
        "L${dottedName.replace('.', '/')};"

    private fun writeAnnotations(
        annotations: List<KvmAnnotation>,
        visitAnnotation: (descriptor: String, visible: Boolean) -> AnnotationVisitor,
    ) {
        annotations.forEach { ann ->
            val av = visitAnnotation(dottedToDescriptor(ann.name), true)
            ann.values.forEach { (key, value) -> writeAnnotationValue(av, key, value) }
            av.visitEnd()
        }
    }

    private fun writeAnnotationValue(av: AnnotationVisitor, name: String?, value: Any) {
        when (value) {
            is List<*> -> {
                val arr = av.visitArray(name)
                value.forEach { writeAnnotationValue(arr, null, it!!) }
                arr.visitEnd()
            }
            is KvmAnnotation -> {
                val nested = av.visitAnnotation(name, dottedToDescriptor(value.name))
                value.values.forEach { (k, v) -> writeAnnotationValue(nested, k, v) }
                nested.visitEnd()
            }
            else -> av.visit(name, value)
        }
    }

    private fun asmLabel(): AsmLabel = AsmLabel()

    private fun generate(body: MethodBody, mv: MethodVisitor) {
        val labelMap = mutableMapOf<Label, AsmLabel>()

        // register exception handlers first — ASM requires this before instructions
        body.handlers.forEach { handler ->
            mv.visitTryCatchBlock(
                labelMap.getOrPut(handler.start) { asmLabel() },
                labelMap.getOrPut(handler.end) { asmLabel() },
                labelMap.getOrPut(handler.handler) { asmLabel() },
                handler.type?.jvmName,   // null = finally
            )
        }

        // emit instructions
        body.instructions.forEach { instruction ->
            emit(labelMap, instruction, mv)
        }
    }

    private fun emit(
        labels: MutableMap<Label, AsmLabel>,
        instruction: Instruction,
        mv: MethodVisitor,
    ) {
        fun label(l: Label): AsmLabel = labels.getOrPut(l) { AsmLabel() }

        when (instruction) {
            // ── pseudo-instructions ─────────────────────────────────────────
            is LabelInstruction -> mv.visitLabel(label(instruction.label))
            is LineNumberInstruction -> {
                // LineNumberInstruction carries no Label of its own — synthesize
                // one marking the current bytecode position.
                val here = AsmLabel()
                mv.visitLabel(here)
                mv.visitLineNumber(instruction.line, here)
            }

            // ── constants ────────────────────────────────────────────────────
            NopInstruction -> mv.visitInsn(Opcodes.NOP)
            AConstNullInstruction -> mv.visitInsn(Opcodes.ACONST_NULL)
            IConstM1Instruction -> mv.visitInsn(Opcodes.ICONST_M1)
            IConst0Instruction -> mv.visitInsn(Opcodes.ICONST_0)
            IConst1Instruction -> mv.visitInsn(Opcodes.ICONST_1)
            IConst2Instruction -> mv.visitInsn(Opcodes.ICONST_2)
            IConst3Instruction -> mv.visitInsn(Opcodes.ICONST_3)
            IConst4Instruction -> mv.visitInsn(Opcodes.ICONST_4)
            IConst5Instruction -> mv.visitInsn(Opcodes.ICONST_5)
            LConst0Instruction -> mv.visitInsn(Opcodes.LCONST_0)
            LConst1Instruction -> mv.visitInsn(Opcodes.LCONST_1)
            FConst0Instruction -> mv.visitInsn(Opcodes.FCONST_0)
            FConst1Instruction -> mv.visitInsn(Opcodes.FCONST_1)
            FConst2Instruction -> mv.visitInsn(Opcodes.FCONST_2)
            DConst0Instruction -> mv.visitInsn(Opcodes.DCONST_0)
            DConst1Instruction -> mv.visitInsn(Opcodes.DCONST_1)
            is BipushInstruction -> mv.visitIntInsn(Opcodes.BIPUSH, instruction.value.toInt())
            is SipushInstruction -> mv.visitIntInsn(Opcodes.SIPUSH, instruction.value.toInt())
            is LdcInstruction -> mv.visitLdcInsn(instruction.value)
            is Ldc2wInstruction -> mv.visitLdcInsn(instruction.value)

            // ── loads ────────────────────────────────────────────────────────
            is ILoadInstruction -> mv.visitVarInsn(Opcodes.ILOAD, instruction.register)
            is LLoadInstruction -> mv.visitVarInsn(Opcodes.LLOAD, instruction.register)
            is FLoadInstruction -> mv.visitVarInsn(Opcodes.FLOAD, instruction.register)
            is DLoadInstruction -> mv.visitVarInsn(Opcodes.DLOAD, instruction.register)
            is ALoadInstruction -> mv.visitVarInsn(Opcodes.ALOAD, instruction.register)

            IALoadInstruction -> mv.visitInsn(Opcodes.IALOAD)
            LALoadInstruction -> mv.visitInsn(Opcodes.LALOAD)
            FALoadInstruction -> mv.visitInsn(Opcodes.FALOAD)
            DALoadInstruction -> mv.visitInsn(Opcodes.DALOAD)
            AALoadInstruction -> mv.visitInsn(Opcodes.AALOAD)
            BALoadInstruction -> mv.visitInsn(Opcodes.BALOAD)
            CALoadInstruction -> mv.visitInsn(Opcodes.CALOAD)
            SALoadInstruction -> mv.visitInsn(Opcodes.SALOAD)

            // ── stores ───────────────────────────────────────────────────────
            is IStoreInstruction -> mv.visitVarInsn(Opcodes.ISTORE, instruction.register)
            is LStoreInstruction -> mv.visitVarInsn(Opcodes.LSTORE, instruction.register)
            is FStoreInstruction -> mv.visitVarInsn(Opcodes.FSTORE, instruction.register)
            is DStoreInstruction -> mv.visitVarInsn(Opcodes.DSTORE, instruction.register)
            is AStoreInstruction -> mv.visitVarInsn(Opcodes.ASTORE, instruction.register)

            IAStoreInstruction -> mv.visitInsn(Opcodes.IASTORE)
            LAStoreInstruction -> mv.visitInsn(Opcodes.LASTORE)
            FAStoreInstruction -> mv.visitInsn(Opcodes.FASTORE)
            DAStoreInstruction -> mv.visitInsn(Opcodes.DASTORE)
            AAStoreInstruction -> mv.visitInsn(Opcodes.AASTORE)
            BAStoreInstruction -> mv.visitInsn(Opcodes.BASTORE)
            CAStoreInstruction -> mv.visitInsn(Opcodes.CASTORE)
            SAStoreInstruction -> mv.visitInsn(Opcodes.SASTORE)

            // ── stack ops ────────────────────────────────────────────────────
            PopInstruction -> mv.visitInsn(Opcodes.POP)
            Pop2Instruction -> mv.visitInsn(Opcodes.POP2)
            DupInstruction -> mv.visitInsn(Opcodes.DUP)
            DupX1Instruction -> mv.visitInsn(Opcodes.DUP_X1)
            DupX2Instruction -> mv.visitInsn(Opcodes.DUP_X2)
            Dup2Instruction -> mv.visitInsn(Opcodes.DUP2)
            Dup2X1Instruction -> mv.visitInsn(Opcodes.DUP2_X1)
            Dup2X2Instruction -> mv.visitInsn(Opcodes.DUP2_X2)
            SwapInstruction -> mv.visitInsn(Opcodes.SWAP)

            // ── arithmetic / conversions / comparisons ──────────────────────
            IAddInstruction -> mv.visitInsn(Opcodes.IADD)
            LAddInstruction -> mv.visitInsn(Opcodes.LADD)
            FAddInstruction -> mv.visitInsn(Opcodes.FADD)
            DAddInstruction -> mv.visitInsn(Opcodes.DADD)
            ISubInstruction -> mv.visitInsn(Opcodes.ISUB)
            LSubInstruction -> mv.visitInsn(Opcodes.LSUB)
            FSubInstruction -> mv.visitInsn(Opcodes.FSUB)
            DSubInstruction -> mv.visitInsn(Opcodes.DSUB)
            IMulInstruction -> mv.visitInsn(Opcodes.IMUL)
            LMulInstruction -> mv.visitInsn(Opcodes.LMUL)
            FMulInstruction -> mv.visitInsn(Opcodes.FMUL)
            DMulInstruction -> mv.visitInsn(Opcodes.DMUL)
            IDivInstruction -> mv.visitInsn(Opcodes.IDIV)
            LDivInstruction -> mv.visitInsn(Opcodes.LDIV)
            FDivInstruction -> mv.visitInsn(Opcodes.FDIV)
            DDivInstruction -> mv.visitInsn(Opcodes.DDIV)
            IRemInstruction -> mv.visitInsn(Opcodes.IREM)
            LRemInstruction -> mv.visitInsn(Opcodes.LREM)
            FRemInstruction -> mv.visitInsn(Opcodes.FREM)
            DRemInstruction -> mv.visitInsn(Opcodes.DREM)
            INegInstruction -> mv.visitInsn(Opcodes.INEG)
            LNegInstruction -> mv.visitInsn(Opcodes.LNEG)
            FNegInstruction -> mv.visitInsn(Opcodes.FNEG)
            DNegInstruction -> mv.visitInsn(Opcodes.DNEG)
            IShlInstruction -> mv.visitInsn(Opcodes.ISHL)
            LShlInstruction -> mv.visitInsn(Opcodes.LSHL)
            IShrInstruction -> mv.visitInsn(Opcodes.ISHR)
            LShrInstruction -> mv.visitInsn(Opcodes.LSHR)
            IUshrInstruction -> mv.visitInsn(Opcodes.IUSHR)
            LUshrInstruction -> mv.visitInsn(Opcodes.LUSHR)
            IAndInstruction -> mv.visitInsn(Opcodes.IAND)
            LAndInstruction -> mv.visitInsn(Opcodes.LAND)
            IOrInstruction -> mv.visitInsn(Opcodes.IOR)
            LOrInstruction -> mv.visitInsn(Opcodes.LOR)
            IXorInstruction -> mv.visitInsn(Opcodes.IXOR)
            LXorInstruction -> mv.visitInsn(Opcodes.LXOR)

            I2LInstruction -> mv.visitInsn(Opcodes.I2L)
            I2FInstruction -> mv.visitInsn(Opcodes.I2F)
            I2DInstruction -> mv.visitInsn(Opcodes.I2D)
            L2IInstruction -> mv.visitInsn(Opcodes.L2I)
            L2FInstruction -> mv.visitInsn(Opcodes.L2F)
            L2DInstruction -> mv.visitInsn(Opcodes.L2D)
            F2IInstruction -> mv.visitInsn(Opcodes.F2I)
            F2LInstruction -> mv.visitInsn(Opcodes.F2L)
            F2DInstruction -> mv.visitInsn(Opcodes.F2D)
            D2IInstruction -> mv.visitInsn(Opcodes.D2I)
            D2LInstruction -> mv.visitInsn(Opcodes.D2L)
            D2FInstruction -> mv.visitInsn(Opcodes.D2F)
            I2BInstruction -> mv.visitInsn(Opcodes.I2B)
            I2CInstruction -> mv.visitInsn(Opcodes.I2C)
            I2SInstruction -> mv.visitInsn(Opcodes.I2S)

            LcmpInstruction -> mv.visitInsn(Opcodes.LCMP)
            FcmplInstruction -> mv.visitInsn(Opcodes.FCMPL)
            FcmpgInstruction -> mv.visitInsn(Opcodes.FCMPG)
            DcmplInstruction -> mv.visitInsn(Opcodes.DCMPL)
            DcmpgInstruction -> mv.visitInsn(Opcodes.DCMPG)

            is IIncInstruction -> mv.visitIincInsn(instruction.index, instruction.amount)

            // ── control flow ─────────────────────────────────────────────────
            is IfEqInstruction -> mv.visitJumpInsn(Opcodes.IFEQ, label(instruction.target))
            is IfNeInstruction -> mv.visitJumpInsn(Opcodes.IFNE, label(instruction.target))
            is IfLtInstruction -> mv.visitJumpInsn(Opcodes.IFLT, label(instruction.target))
            is IfGeInstruction -> mv.visitJumpInsn(Opcodes.IFGE, label(instruction.target))
            is IfGtInstruction -> mv.visitJumpInsn(Opcodes.IFGT, label(instruction.target))
            is IfLeInstruction -> mv.visitJumpInsn(Opcodes.IFLE, label(instruction.target))
            is IfICmpEqInstruction -> mv.visitJumpInsn(Opcodes.IF_ICMPEQ, label(instruction.target))
            is IfICmpNeInstruction -> mv.visitJumpInsn(Opcodes.IF_ICMPNE, label(instruction.target))
            is IfICmpLtInstruction -> mv.visitJumpInsn(Opcodes.IF_ICMPLT, label(instruction.target))
            is IfICmpGeInstruction -> mv.visitJumpInsn(Opcodes.IF_ICMPGE, label(instruction.target))
            is IfICmpGtInstruction -> mv.visitJumpInsn(Opcodes.IF_ICMPGT, label(instruction.target))
            is IfICmpLeInstruction -> mv.visitJumpInsn(Opcodes.IF_ICMPLE, label(instruction.target))
            is IfACmpEqInstruction -> mv.visitJumpInsn(Opcodes.IF_ACMPEQ, label(instruction.target))
            is IfACmpNeInstruction -> mv.visitJumpInsn(Opcodes.IF_ACMPNE, label(instruction.target))
            is IfNullInstruction -> mv.visitJumpInsn(Opcodes.IFNULL, label(instruction.target))
            is IfNonNullInstruction -> mv.visitJumpInsn(Opcodes.IFNONNULL, label(instruction.target))
            is GotoInstruction -> mv.visitJumpInsn(Opcodes.GOTO, label(instruction.target))

            JsrInstruction -> error("JSR is deprecated and unsupported (no target label modeled); illegal in class files >= v51")
            RetInstruction -> error("RET is deprecated and unsupported (no register modeled); illegal in class files >= v51")

            is TableSwitchInstruction -> {
                val sortedKeys = instruction.cases.keys.sorted()
                val min = sortedKeys.first()
                val max = sortedKeys.last()
                val ordered = (min..max).map { key ->
                    instruction.cases[key]?.let { label(it) }
                        ?: error("TABLESWITCH requires contiguous keys; missing case for $key")
                }
                mv.visitTableSwitchInsn(min, max, label(instruction.default), *ordered.toTypedArray())
            }

            is LookupSwitchInstruction -> {
                val keys = instruction.cases.keys.sorted().toIntArray()
                val asmLabels = keys.map { label(instruction.cases.getValue(it)) }.toTypedArray()
                mv.visitLookupSwitchInsn(label(instruction.default), keys, asmLabels)
            }

            // ── returns ──────────────────────────────────────────────────────
            IReturnInstruction -> mv.visitInsn(Opcodes.IRETURN)
            LReturnInstruction -> mv.visitInsn(Opcodes.LRETURN)
            FReturnInstruction -> mv.visitInsn(Opcodes.FRETURN)
            DReturnInstruction -> mv.visitInsn(Opcodes.DRETURN)
            AReturnInstruction -> mv.visitInsn(Opcodes.ARETURN)
            VReturnInstruction -> mv.visitInsn(Opcodes.RETURN)

            // ── fields ───────────────────────────────────────────────────────
            is GetStaticInstruction -> mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                instruction.field.owner.jvmName,
                instruction.field.name,
                instruction.field.type.jvmName,
            )

            is PutStaticInstruction -> mv.visitFieldInsn(
                Opcodes.PUTSTATIC,
                instruction.field.owner.jvmName,
                instruction.field.name,
                instruction.field.type.jvmName,
            )

            is GetFieldInstruction -> mv.visitFieldInsn(
                Opcodes.GETFIELD,
                instruction.field.owner.jvmName,
                instruction.field.name,
                instruction.field.type.jvmName,
            )

            is PutFieldInstruction -> mv.visitFieldInsn(
                Opcodes.PUTFIELD,
                instruction.field.owner.jvmName,
                instruction.field.name,
                instruction.field.type.jvmName,
            )

            // ── method invocation ────────────────────────────────────────────
            is InvokeVirtualInstruction -> mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, instruction.owner.jvmName, instruction.method.name,
                buildMethodDescriptor(instruction.method), false,
            )

            is InvokeSpecialInstruction -> mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, instruction.owner.jvmName, instruction.method.name,
                buildMethodDescriptor(instruction.method), false,
            )

            is InvokeStaticInstruction -> mv.visitMethodInsn(
                Opcodes.INVOKESTATIC, instruction.owner.jvmName, instruction.method.name,
                buildMethodDescriptor(instruction.method), false,
            )

            is InvokeInterfaceInstruction -> mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE, instruction.owner.jvmName, instruction.method.name,
                buildMethodDescriptor(instruction.method), true,
            )

            is InvokeDynamicInstruction -> {
                // TODO mv.visitInvokeDynamicInsn()
                error("INVOKEDYNAMIC requires a bootstrap method — handle separately")
            }

            // ── type / array operations ──────────────────────────────────────
            is NewInstruction -> mv.visitTypeInsn(Opcodes.NEW, instruction.type.jvmName)
            is NewArrayInstruction -> {
                if (instruction.dimensions == 1) {
                    when (instruction.elementType) {
                        IntType -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT)
                        LongType -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG)
                        FloatType -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT)
                        DoubleType -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE)
                        BooleanType -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN)
                        ByteType -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE)
                        CharType -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_CHAR)
                        ShortType -> mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_SHORT)
                        is ClassType -> mv.visitTypeInsn(Opcodes.ANEWARRAY, instruction.elementType.jvmName)
                        else -> error("Unsupported array element type: ${instruction.elementType}")
                    }
                } else {
                    var ty = instruction.elementType
                    repeat(instruction.dimensions) { ty = ArrayType(ty) }
                    mv.visitMultiANewArrayInsn(ty.jvmName, instruction.dimensions)
                }
            }

            is ANewArrayInstruction -> mv.visitTypeInsn(Opcodes.ANEWARRAY, instruction.elementType.jvmName)
            ArrayLengthInstruction -> mv.visitInsn(Opcodes.ARRAYLENGTH)
            AThrowInstruction -> mv.visitInsn(Opcodes.ATHROW)
            is CheckCastInstruction -> mv.visitTypeInsn(Opcodes.CHECKCAST, instruction.type.jvmName)
            is InstanceOfInstruction -> mv.visitTypeInsn(Opcodes.INSTANCEOF, instruction.type.jvmName)
            MonitorEnterInstruction -> mv.visitInsn(Opcodes.MONITORENTER)
            MonitorExitInstruction -> mv.visitInsn(Opcodes.MONITOREXIT)
            is MultiANewArrayInstruction -> {
                var ty = instruction.elementType
                repeat(instruction.dimensions) { ty = ArrayType(ty) }
                mv.visitMultiANewArrayInsn(ty.jvmName, instruction.dimensions)
            }
        }
    }

    override fun parse(bytes: ByteArray): ClassDeclaration {
        return asmParse(bytes)
    }

// ── helpers ──────────────────────────────────────────────────────────────

    private fun buildMethodDescriptor(sig: MethodSignature): String {
        val params = sig.parameterTypes.joinToString("") { it.jvmName }
        return "($params)${sig.returnType.jvmName}"
    }

    // ── entry point ──────────────────────────────────────────────────────────
// Add to `object ASM : BytecodeGenerator, BytecodeParser { ... }`:
//
//     override fun parse(bytes: ByteArray): ClassDeclaration = asmParse(bytes)

    private fun asmParse(bytes: ByteArray): ClassDeclaration {
        val reader = ClassReader(bytes)
        val visitor = KvmClassVisitor()
        reader.accept(visitor, ClassReader.SKIP_FRAMES) // frames are recomputed by generate() anyway
        return visitor.toClassDeclaration()
    }

// ── type conversion helpers ─────────────────────────────────────────────

    private fun internalToDotted(internalName: String): String = internalName.replace('/', '.')

    private fun asmTypeToDomain(t: AsmType): Type = when (t.sort) {
        AsmType.VOID -> VoidType
        AsmType.BOOLEAN -> BooleanType
        AsmType.CHAR -> CharType
        AsmType.BYTE -> ByteType
        AsmType.SHORT -> ShortType
        AsmType.INT -> IntType
        AsmType.FLOAT -> FloatType
        AsmType.LONG -> LongType
        AsmType.DOUBLE -> DoubleType
        AsmType.ARRAY -> {
            var result = asmTypeToDomain(t.elementType)
            repeat(t.dimensions) { result = ArrayType(result) }
            result
        }

        AsmType.OBJECT -> ClassType(internalToDotted(t.internalName))
        else -> error("Unsupported ASM type sort: ${t.sort} (${t.descriptor})")
    }

    private fun descriptorToMethodSignature(name: String, descriptor: String): MethodSignature {
        val paramTypes = AsmType.getArgumentTypes(descriptor).map { asmTypeToDomain(it) }
        val returnType = asmTypeToDomain(AsmType.getReturnType(descriptor))
        return MethodSignature(name, MethodDescriptor(paramTypes, returnType))
    }

    private fun descriptorToFieldType(descriptor: String): Type = asmTypeToDomain(AsmType.getType(descriptor))

    /** Resolves an ANEWARRAY-style internal name/descriptor to a domain Type (handles array descriptors too). */
    private fun anewarrayOperandToType(type: String): Type =
        if (type.startsWith("[")) asmTypeToDomain(AsmType.getType(type)) else ClassType(internalToDotted(type))

    /** CHECKCAST/INSTANCEOF operands must resolve to a ClassType; array-type operands aren't representable
     *  by CheckCastInstruction/InstanceOfInstruction as currently modeled (both are typed to ClassType only). */
    private fun requireClassType(type: Type): ReferenceType {
        check(type is ReferenceType) {
            "CHECKCAST/INSTANCEOF against array type '$type' isn't representable — " +
                    "CheckCastInstruction/InstanceOfInstruction are typed to ClassType only. " +
                    "Widen those instructions to accept Type if you need to support this."
        }
        return type
    }

    /** Strips one leading '[' dimension marker layer per call; used for MULTIANEWARRAY base element type. */
    private fun multiANewArrayElementType(descriptor: String, dimensions: Int): Type {
        val baseDescriptor = descriptor.removePrefix("[".repeat(dimensions))
        return asmTypeToDomain(AsmType.getType(baseDescriptor))
    }

// ── annotation value collection (best-effort: primitives/strings only) ──

    private class KvmAnnotationValueCollector(private val target: MutableMap<String, Any>) :
        AnnotationVisitor(Opcodes.ASM9) {
        override fun visit(name: String?, value: Any?) {
            if (name != null && value != null) target[name] = value
        }
        // enum/array/nested-annotation values are not modeled yet — extend here if needed
    }

// ── class visitor ───────────────────────────────────────────────────────

    private class KvmClassVisitor : ClassVisitor(Opcodes.ASM9) {

        private lateinit var classType: ClassType
        private var superClass: ClassType? = null
        private var interfaces: List<ClassType> = emptyList()
        private var accessFlags: ClassFlags = ClassFlags.EMPTY
        private val annotations = mutableListOf<KvmAnnotation>()
        private val fields = mutableListOf<ConcreteField>()
        private val methods = mutableListOf<PresentMethodDeclaration>()

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?,
        ) {
            this.classType = ClassType(internalToDotted(name))
            this.superClass = superName?.let { ClassType(internalToDotted(it)) }
            this.interfaces = interfaces?.map { ClassType(internalToDotted(it)) } ?: emptyList()
            this.accessFlags = ClassFlags(access)
        }

        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
            val name = AsmType.getType(descriptor).className
            val values = mutableMapOf<String, Any>()
            annotations += KvmAnnotation(name, values)
            return KvmAnnotationValueCollector(values)
        }

        override fun visitField(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            value: Any?,
        ): FieldVisitor {
            val fieldSignature = FieldSignature(classType, name, descriptorToFieldType(descriptor))
            fields += ConcreteField(fieldSignature, FieldFlags(access), listOf())
            return object : FieldVisitor(Opcodes.ASM9) {} // field annotations skipped for now
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            val methodSignature = descriptorToMethodSignature(name, descriptor)
            val flags = MethodFlags(access)
            val isAbstractOrNative = flags.isAbstract || flags.isNative

            return KvmMethodVisitor(classType, methodSignature, flags, isAbstractOrNative) { method ->
                methods += method
            }
        }

        fun toClassDeclaration(): ClassDeclaration = ConcreteClass(
            type = classType,
            superClass = superClass,
            interfaces = interfaces,
            annotations = annotations,
            methods = methods,
            fields = fields,
            accessFlags = accessFlags,
        )
    }

// ── method body collection ────────────────────────────────────────────

    private class KvmMethodVisitor(
        private val owner: ClassType,
        private val signature: MethodSignature,
        private val flags: MethodFlags,
        private val isAbstractOrNative: Boolean,
        private val onComplete: (PresentMethodDeclaration) -> Unit,
    ) : MethodVisitor(Opcodes.ASM9) {

        private val annotations = mutableListOf<KvmAnnotation>()
        private val instructions = mutableListOf<Instruction>()
        private val handlers = mutableListOf<ExceptionHandler>()
        private val labelMap = mutableMapOf<AsmLabel, Label>()

        private val definedLabels = mutableMapOf<AsmLabel, Instruction>()
        private val referencedLabels = mutableSetOf<AsmLabel>()

        private fun label(l: AsmLabel): Label = labelMap.getOrPut(l) { Label() }

        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
            val name = AsmType.getType(descriptor).className
            val values = mutableMapOf<String, Any>()
            annotations += KvmAnnotation(name, values)
            return KvmAnnotationValueCollector(values)
        }

        override fun visitTryCatchBlock(start: AsmLabel, end: AsmLabel, handler: AsmLabel, type: String?) {
            handlers += ExceptionHandler(
                start = label(start),
                end = label(end),
                handler = label(handler),
                type = type?.let { ClassType(internalToDotted(it)) },
            )
            referencedLabels += start
            referencedLabels += end
            referencedLabels += handler
        }

        override fun visitLabel(l: AsmLabel) {
            val inst = LabelInstruction(label(l))
            instructions += inst
            definedLabels[l] = inst
        }

        override fun visitLineNumber(line: Int, start: AsmLabel) {
            instructions += LineNumberInstruction(line)
        }

        override fun visitInsn(opcode: Int) {
            instructions += when (opcode) {
                Opcodes.NOP -> NopInstruction
                Opcodes.ACONST_NULL -> AConstNullInstruction
                Opcodes.ICONST_M1 -> IConstM1Instruction
                Opcodes.ICONST_0 -> IConst0Instruction
                Opcodes.ICONST_1 -> IConst1Instruction
                Opcodes.ICONST_2 -> IConst2Instruction
                Opcodes.ICONST_3 -> IConst3Instruction
                Opcodes.ICONST_4 -> IConst4Instruction
                Opcodes.ICONST_5 -> IConst5Instruction
                Opcodes.LCONST_0 -> LConst0Instruction
                Opcodes.LCONST_1 -> LConst1Instruction
                Opcodes.FCONST_0 -> FConst0Instruction
                Opcodes.FCONST_1 -> FConst1Instruction
                Opcodes.FCONST_2 -> FConst2Instruction
                Opcodes.DCONST_0 -> DConst0Instruction
                Opcodes.DCONST_1 -> DConst1Instruction
                Opcodes.IALOAD -> IALoadInstruction
                Opcodes.LALOAD -> LALoadInstruction
                Opcodes.FALOAD -> FALoadInstruction
                Opcodes.DALOAD -> DALoadInstruction
                Opcodes.AALOAD -> AALoadInstruction
                Opcodes.BALOAD -> BALoadInstruction
                Opcodes.CALOAD -> CALoadInstruction
                Opcodes.SALOAD -> SALoadInstruction
                Opcodes.IASTORE -> IAStoreInstruction
                Opcodes.LASTORE -> LAStoreInstruction
                Opcodes.FASTORE -> FAStoreInstruction
                Opcodes.DASTORE -> DAStoreInstruction
                Opcodes.AASTORE -> AAStoreInstruction
                Opcodes.BASTORE -> BAStoreInstruction
                Opcodes.CASTORE -> CAStoreInstruction
                Opcodes.SASTORE -> SAStoreInstruction
                Opcodes.POP -> PopInstruction
                Opcodes.POP2 -> Pop2Instruction
                Opcodes.DUP -> DupInstruction
                Opcodes.DUP_X1 -> DupX1Instruction
                Opcodes.DUP_X2 -> DupX2Instruction
                Opcodes.DUP2 -> Dup2Instruction
                Opcodes.DUP2_X1 -> Dup2X1Instruction
                Opcodes.DUP2_X2 -> Dup2X2Instruction
                Opcodes.SWAP -> SwapInstruction
                Opcodes.IADD -> IAddInstruction
                Opcodes.LADD -> LAddInstruction
                Opcodes.FADD -> FAddInstruction
                Opcodes.DADD -> DAddInstruction
                Opcodes.ISUB -> ISubInstruction
                Opcodes.LSUB -> LSubInstruction
                Opcodes.FSUB -> FSubInstruction
                Opcodes.DSUB -> DSubInstruction
                Opcodes.IMUL -> IMulInstruction
                Opcodes.LMUL -> LMulInstruction
                Opcodes.FMUL -> FMulInstruction
                Opcodes.DMUL -> DMulInstruction
                Opcodes.IDIV -> IDivInstruction
                Opcodes.LDIV -> LDivInstruction
                Opcodes.FDIV -> FDivInstruction
                Opcodes.DDIV -> DDivInstruction
                Opcodes.IREM -> IRemInstruction
                Opcodes.LREM -> LRemInstruction
                Opcodes.FREM -> FRemInstruction
                Opcodes.DREM -> DRemInstruction
                Opcodes.INEG -> INegInstruction
                Opcodes.LNEG -> LNegInstruction
                Opcodes.FNEG -> FNegInstruction
                Opcodes.DNEG -> DNegInstruction
                Opcodes.ISHL -> IShlInstruction
                Opcodes.LSHL -> LShlInstruction
                Opcodes.ISHR -> IShrInstruction
                Opcodes.LSHR -> LShrInstruction
                Opcodes.IUSHR -> IUshrInstruction
                Opcodes.LUSHR -> LUshrInstruction
                Opcodes.IAND -> IAndInstruction
                Opcodes.LAND -> LAndInstruction
                Opcodes.IOR -> IOrInstruction
                Opcodes.LOR -> LOrInstruction
                Opcodes.IXOR -> IXorInstruction
                Opcodes.LXOR -> LXorInstruction
                Opcodes.I2L -> I2LInstruction
                Opcodes.I2F -> I2FInstruction
                Opcodes.I2D -> I2DInstruction
                Opcodes.L2I -> L2IInstruction
                Opcodes.L2F -> L2FInstruction
                Opcodes.L2D -> L2DInstruction
                Opcodes.F2I -> F2IInstruction
                Opcodes.F2L -> F2LInstruction
                Opcodes.F2D -> F2DInstruction
                Opcodes.D2I -> D2IInstruction
                Opcodes.D2L -> D2LInstruction
                Opcodes.D2F -> D2FInstruction
                Opcodes.I2B -> I2BInstruction
                Opcodes.I2C -> I2CInstruction
                Opcodes.I2S -> I2SInstruction
                Opcodes.LCMP -> LcmpInstruction
                Opcodes.FCMPL -> FcmplInstruction
                Opcodes.FCMPG -> FcmpgInstruction
                Opcodes.DCMPL -> DcmplInstruction
                Opcodes.DCMPG -> DcmpgInstruction
                Opcodes.IRETURN -> IReturnInstruction
                Opcodes.LRETURN -> LReturnInstruction
                Opcodes.FRETURN -> FReturnInstruction
                Opcodes.DRETURN -> DReturnInstruction
                Opcodes.ARETURN -> AReturnInstruction
                Opcodes.RETURN -> VReturnInstruction
                Opcodes.ARRAYLENGTH -> ArrayLengthInstruction
                Opcodes.ATHROW -> AThrowInstruction
                Opcodes.MONITORENTER -> MonitorEnterInstruction
                Opcodes.MONITOREXIT -> MonitorExitInstruction
                else -> error("Unhandled zero-operand opcode: $opcode")
            }
        }

        override fun visitIntInsn(opcode: Int, operand: Int) {
            instructions += when (opcode) {
                Opcodes.BIPUSH -> BipushInstruction(operand.toByte())
                Opcodes.SIPUSH -> SipushInstruction(operand.toShort())
                Opcodes.NEWARRAY -> {
                    val elementType = when (operand) {
                        Opcodes.T_BOOLEAN -> BooleanType
                        Opcodes.T_CHAR -> CharType
                        Opcodes.T_FLOAT -> FloatType
                        Opcodes.T_DOUBLE -> DoubleType
                        Opcodes.T_BYTE -> ByteType
                        Opcodes.T_SHORT -> ShortType
                        Opcodes.T_INT -> IntType
                        Opcodes.T_LONG -> LongType
                        else -> error("Unknown NEWARRAY primitive code: $operand")
                    }
                    NewArrayInstruction(elementType, dimensions = 1)
                }

                else -> error("Unhandled int-operand opcode: $opcode")
            }
        }

        override fun visitVarInsn(opcode: Int, varIndex: Int) {
            instructions += when (opcode) {
                Opcodes.ILOAD -> ILoadInstruction(varIndex)
                Opcodes.LLOAD -> LLoadInstruction(varIndex)
                Opcodes.FLOAD -> FLoadInstruction(varIndex)
                Opcodes.DLOAD -> DLoadInstruction(varIndex)
                Opcodes.ALOAD -> ALoadInstruction(varIndex)
                Opcodes.ISTORE -> IStoreInstruction(varIndex)
                Opcodes.LSTORE -> LStoreInstruction(varIndex)
                Opcodes.FSTORE -> FStoreInstruction(varIndex)
                Opcodes.DSTORE -> DStoreInstruction(varIndex)
                Opcodes.ASTORE -> AStoreInstruction(varIndex)
                Opcodes.RET -> error("RET is deprecated/unsupported (illegal in class files >= v51)")
                else -> error("Unhandled var-operand opcode: $opcode")
            }
        }

        override fun visitTypeInsn(opcode: Int, type: String) {
            instructions += when (opcode) {
                Opcodes.NEW -> NewInstruction(ClassType(internalToDotted(type)))
                Opcodes.ANEWARRAY -> ANewArrayInstruction(anewarrayOperandToType(type), dimensions = 1)
                Opcodes.CHECKCAST -> CheckCastInstruction(requireClassType("L$type;".parseJvmName()))
                Opcodes.INSTANCEOF -> InstanceOfInstruction(requireClassType("L$type;".parseJvmName()))
                else -> error("Unhandled type-operand opcode: $opcode")
            }
        }

        override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
            val fieldSig = FieldSignature(ClassType(internalToDotted(owner)), name, descriptorToFieldType(descriptor))
            instructions += when (opcode) {
                Opcodes.GETSTATIC -> GetStaticInstruction(fieldSig)
                Opcodes.PUTSTATIC -> PutStaticInstruction(fieldSig)
                Opcodes.GETFIELD -> GetFieldInstruction(fieldSig)
                Opcodes.PUTFIELD -> PutFieldInstruction(fieldSig)
                else -> error("Unhandled field opcode: $opcode")
            }
        }

        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean,
        ) {
            val ownerType = ClassType(internalToDotted(owner))
            val methodSig = descriptorToMethodSignature(name, descriptor)
            instructions += when (opcode) {
                Opcodes.INVOKEVIRTUAL -> InvokeVirtualInstruction(ownerType, methodSig)
                Opcodes.INVOKESPECIAL -> InvokeSpecialInstruction(ownerType, methodSig)
                Opcodes.INVOKESTATIC -> InvokeStaticInstruction(ownerType, methodSig)
                Opcodes.INVOKEINTERFACE -> InvokeInterfaceInstruction(ownerType, methodSig)
                else -> error("Unhandled method opcode: $opcode")
            }
        }

        override fun visitInvokeDynamicInsn(
            name: String,
            descriptor: String,
            bootstrapMethodHandle: Handle,
            vararg bootstrapMethodArguments: Any,
        ) {
            instructions += InvokeDynamicInstruction(
                callSiteSignature = descriptorToMethodSignature(name, descriptor),
                bootstrapMethod = bootstrapMethodHandle.toMethodHandle(),
                bootstrapArguments = bootstrapMethodArguments.map { it.toBootstrapArgument() }
            )
        }

        private fun Handle.toMethodHandle(): com.leko.kvm.bytecode.MethodHandle {
            val kind = HandleKind.entries.firstOrNull { it.opcode.toInt() == tag }
            if (kind == null) {
                error("unexpected Method Handle Kind: $tag")
            }
            return MethodHandle(
                kind = kind,
                owner = ClassType(owner.replace("/", ".")),
                name = name,
                descriptor = desc,
                isInterface = isInterface,
            )
        }

        private fun Any.toBootstrapArgument(): BootstrapArgument = when (this) {
            is Int -> BootstrapArgument.IntArg(this)
            is Float -> BootstrapArgument.FloatArg(this)
            is Long -> BootstrapArgument.LongArg(this)
            is Double -> BootstrapArgument.DoubleArg(this)
            is String -> BootstrapArgument.StringArg(this)
            is AsmType -> {
                when (sort) {
                    AsmType.METHOD -> BootstrapArgument.MethodTypeArg(toMethodDescriptorModel())
                    else -> BootstrapArgument.TypeArg(className.parseJvmName() as ClassType)
                }
            }
            is Handle -> BootstrapArgument.HandleArg(this.toMethodHandle())
            is org.objectweb.asm.ConstantDynamic -> BootstrapArgument.DynamicArg(this.toConstantDynamicModel())
            else -> error("Unsupported bootstrap argument type: ${this::class.qualifiedName}")
        }

        private fun AsmType.toMethodDescriptorModel(): MethodDescriptor {
            require(sort == AsmType.METHOD) {
                "Expected a METHOD-sorted Type, got sort=$sort ($descriptor)"
            }
            return MethodDescriptor(
                parameterTypes = argumentTypes.map { asmTypeToDomain(it) },
                returnType = asmTypeToDomain(returnType),
            )
        }

        private fun org.objectweb.asm.ConstantDynamic.toConstantDynamicModel(): ConstantDynamic =
            ConstantDynamic(
                name = name,
                type = descriptor.parseJavaName(),
                bootstrapMethod = bootstrapMethod.toMethodHandle(),
                bootstrapArguments = (0 ..< bootstrapMethodArgumentCount)
                    .map { getBootstrapMethodArgument(it).toBootstrapArgument() }
            )

        override fun visitJumpInsn(opcode: Int, l: AsmLabel) {
            val target = label(l)
            referencedLabels += l
            instructions += when (opcode) {
                Opcodes.IFEQ -> IfEqInstruction(target)
                Opcodes.IFNE -> IfNeInstruction(target)
                Opcodes.IFLT -> IfLtInstruction(target)
                Opcodes.IFGE -> IfGeInstruction(target)
                Opcodes.IFGT -> IfGtInstruction(target)
                Opcodes.IFLE -> IfLeInstruction(target)
                Opcodes.IF_ICMPEQ -> IfICmpEqInstruction(target)
                Opcodes.IF_ICMPNE -> IfICmpNeInstruction(target)
                Opcodes.IF_ICMPLT -> IfICmpLtInstruction(target)
                Opcodes.IF_ICMPGE -> IfICmpGeInstruction(target)
                Opcodes.IF_ICMPGT -> IfICmpGtInstruction(target)
                Opcodes.IF_ICMPLE -> IfICmpLeInstruction(target)
                Opcodes.IF_ACMPEQ -> IfACmpEqInstruction(target)
                Opcodes.IF_ACMPNE -> IfACmpNeInstruction(target)
                Opcodes.IFNULL -> IfNullInstruction(target)
                Opcodes.IFNONNULL -> IfNonNullInstruction(target)
                Opcodes.GOTO -> GotoInstruction(target)
                Opcodes.JSR -> error("JSR is deprecated/unsupported")
                else -> error("Unhandled jump opcode: $opcode")
            }
        }

        override fun visitLdcInsn(value: Any) {
            instructions += if (value is Long || value is Double) {
                Ldc2wInstruction(value as Number)
            } else {
                LdcInstruction(value)
            }
        }

        override fun visitIincInsn(varIndex: Int, increment: Int) {
            instructions += IIncInstruction(varIndex, increment)
        }

        override fun visitTableSwitchInsn(min: Int, max: Int, dflt: AsmLabel, vararg labels: AsmLabel) {
            val cases = (min..max).zip(labels).associate { (key, l) -> key to label(l) }
            instructions += TableSwitchInstruction(label(dflt), cases)
            referencedLabels += dflt
            referencedLabels += labels
        }

        override fun visitLookupSwitchInsn(dflt: AsmLabel, keys: IntArray, labels: Array<out AsmLabel>) {
            val cases = keys.zip(labels).associate { (key, l) -> key to label(l) }
            instructions += LookupSwitchInstruction(label(dflt), cases)
            referencedLabels += dflt
            referencedLabels += labels
        }

        override fun visitMultiANewArrayInsn(descriptor: String, numDimensions: Int) {
            instructions += MultiANewArrayInstruction(
                elementType = multiANewArrayElementType(descriptor, numDimensions),
                dimensions = numDimensions,
            )
        }

        override fun visitEnd() {
            val debugOnlyInstructions = definedLabels.filterKeys { it !in referencedLabels }.values.toSet()
            val method = if (isAbstractOrNative) {
                AbstractMethod(owner, signature, flags, annotations)
            } else {
                ConcreteMethod(
                    owner = owner,
                    signature = signature,
                    accessFlags = flags,
                    annotations = annotations,
                    body = MethodBody(instructions.filter { it !in debugOnlyInstructions }, handlers.toList()),
                )
            }
            onComplete(method)
        }

    }
}