package com.leko.kvm.bytecode

import com.leko.kvm.ClassFlags
import com.leko.kvm.ConcreteClass
import com.leko.kvm.FieldFlags
import com.leko.kvm.KvmAnnotation
import com.leko.kvm.MethodFlags
import com.leko.kvm.PresentMethodDeclaration
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.Type
import com.leko.kvm.typing.VoidType

/**
 * Builder for completing Annotations
 */
interface AnnotationBuilder {

    fun put(field: String, value: Byte): AnnotationBuilder

    fun put(field: String, value: Char): AnnotationBuilder

    fun put(field: String, value: Short): AnnotationBuilder

    fun put(field: String, value: Int): AnnotationBuilder

    fun put(field: String, value: Long): AnnotationBuilder

    fun put(field: String, value: Float): AnnotationBuilder

    fun put(field: String, value: Double): AnnotationBuilder

    fun put(field: String, value: String): AnnotationBuilder

    fun put(field: String, value: Boolean): AnnotationBuilder

    fun put(field: String, value: ClassType): AnnotationBuilder

    fun put(field: String, value: ByteArray): AnnotationBuilder

    fun put(field: String, value: CharArray): AnnotationBuilder

    fun put(field: String, value: ShortArray): AnnotationBuilder

    fun put(field: String, value: IntArray): AnnotationBuilder

    fun put(field: String, value: LongArray): AnnotationBuilder

    fun put(field: String, value: FloatArray): AnnotationBuilder

    fun put(field: String, value: DoubleArray): AnnotationBuilder

    fun put(field: String, value: Array<String>): AnnotationBuilder

    fun put(field: String, value: BooleanArray): AnnotationBuilder

    fun put(field: String, value: Array<ClassType>): AnnotationBuilder

}

interface CompletableAnnotationBuilder: AnnotationBuilder {

    /**
     * Completes the built annotation.
     */
    fun build(): KvmAnnotation


}

/**
 * Builder for building class files
 */
interface ClassBuilder {

    /**
     * The flags of the generated class
     */
    var flags: ClassFlags

    /**
     * The super class. Is java.lang.Object by default.
     */
    var superClass: ClassType

    /**
     * The interfaces the generated class implements
     */
    var interfaces: List<ClassType>

    /**
     * Attaches an annotation to the generated class
     */
    fun annotation(name: String): AnnotationBuilder

    /**
     * Creates a builder with which a new method can be defined for this class
     */
    fun method(name: String, returnType: Type = VoidType): MethodBuilder

    /**
     * Creates a builder with which a new constructor can be defined for this class
     */
    fun constructor(): MethodBuilder

    /**
     * Creates a builder with which a static initializer block can be defined for this class
     */
    fun static(): Pair<ControlFlowBuilder, ValueBuilder>

    /**
     * Adds a field to this class
     */
    fun field(name: String, type: Type, accessFlags: FieldFlags = FieldFlags.EMPTY): ClassBuilder

}

/**
 * A completable class builder that can build the final [com.leko.kvm.ConcreteClass].
 */
interface CompletableClassBuilder: ClassBuilder {

    /**
     * Completes the built class.
     */
    fun build(): ConcreteClass

}

/**
 * Builder for defining the signature and implementation for a method.
 */
interface MethodBuilder {

    /**
     * The flags of this method.
     */
    var flags: MethodFlags

    /**
     * Attaches an annotation to the generated method
     */
    fun annotation(name: String): AnnotationBuilder

    /**
     * The parameters of this method.
     */
    fun parameters(): ParametersBuilder

    /**
     * Provides the builders required for defining the body of this method.
     *
     * Use the [ControlFlowBuilder] for defining the actual control flow
     * and the [ValueBuilder] for building the values used in that flow.
     */
    fun body(): Pair<ControlFlowBuilder, ValueBuilder>
}

interface CompletableMethodBuilder: MethodBuilder {

    /**
     * Completes the build method
     */
    fun build(): PresentMethodDeclaration

}

interface ParametersBuilder {

    fun parameter(name: String, type: Type): ParametersBuilder
}

interface ControlFlowBuilder {

    fun line(n: Int): ControlFlowBuilder

    fun placeLabel(label: Label): ControlFlowBuilder

    fun goto(label: Label): ControlFlowBuilder

    fun branch(condition: Value): ScopedControlFlowBuilder<BranchCompleter>

    fun eval(value: Value): ControlFlowBuilder

    fun returns(value: Value? = null): ControlFlowBuilder

    fun throws(value: Value): ControlFlowBuilder

    fun store(ptr: Ptr, value: Value): ControlFlowBuilder

    fun switch(value: Value): SwitchBuilder

    fun attempt(): ScopedControlFlowBuilder<AttemptCompleter>

    fun superCall(vararg arguments: Value): ControlFlowBuilder

    fun instruction(instruction: Instruction): ControlFlowBuilder
}

abstract class ScopedControlFlowBuilder<R>(flow: ControlFlowBuilder): ControlFlowBuilder by flow {

    abstract fun close(): R

}

interface BranchCompleter {

    fun otherwise(): ScopedControlFlowBuilder<ControlFlowBuilder>

    fun complete(): ControlFlowBuilder

}

interface ValueBuilder {

    companion object {
        val NULL: Value = NullValue

        val TRUE: Value = BooleanValue(true)

        val FALSE: Value = BooleanValue(false)
    }

    fun label(): Label

    fun parameter(name: String): LocalPtr

    fun local(type: Type): LocalPtr

    val THIS: Value

}


interface SwitchBuilder {

    fun case(key: Int): ScopedControlFlowBuilder<SwitchBuilder>

    fun otherwise(): ScopedControlFlowBuilder<ControlFlowBuilder>

    fun complete(): ControlFlowBuilder

}

interface AttemptCompleter {

    fun rescue(type: ClassType): ScopedControlFlowBuilder<AttemptCompleter>

    fun finally(): ScopedControlFlowBuilder<ControlFlowBuilder>

    fun complete(): ControlFlowBuilder
}
