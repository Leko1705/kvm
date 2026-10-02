package com.leko.kvm.bytecode

import com.leko.kvm.ClassFlags
import com.leko.kvm.ConcreteClass
import com.leko.kvm.ConcreteField
import com.leko.kvm.FieldFlags
import com.leko.kvm.KvmAnnotation
import com.leko.kvm.MethodFlags
import com.leko.kvm.PresentMethodDeclaration
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.Type
import com.leko.kvm.typing.VoidType

/**
 * Builder for defining the element values of an annotation.
 *
 * Annotation element values are stored by their element name. Implementations
 * support JVM annotation-compatible primitive values, strings, class types,
 * and arrays of those values.
 *
 * All [put] functions return this builder, allowing multiple annotation
 * elements to be configured fluently.
 */
interface AnnotationBuilder {

    /** Sets a byte-valued annotation element. */
    fun put(field: String, value: Byte): AnnotationBuilder

    /** Sets a character-valued annotation element. */
    fun put(field: String, value: Char): AnnotationBuilder

    /** Sets a short-valued annotation element. */
    fun put(field: String, value: Short): AnnotationBuilder

    /** Sets an integer-valued annotation element. */
    fun put(field: String, value: Int): AnnotationBuilder

    /** Sets a long-valued annotation element. */
    fun put(field: String, value: Long): AnnotationBuilder

    /** Sets a float-valued annotation element. */
    fun put(field: String, value: Float): AnnotationBuilder

    /** Sets a double-valued annotation element. */
    fun put(field: String, value: Double): AnnotationBuilder

    /** Sets a string-valued annotation element. */
    fun put(field: String, value: String): AnnotationBuilder

    /** Sets a boolean-valued annotation element. */
    fun put(field: String, value: Boolean): AnnotationBuilder

    /** Sets a class-valued annotation element. */
    fun put(field: String, value: ClassType): AnnotationBuilder

    /** Sets a byte-array-valued annotation element. */
    fun put(field: String, value: ByteArray): AnnotationBuilder

    /** Sets a character-array-valued annotation element. */
    fun put(field: String, value: CharArray): AnnotationBuilder

    /** Sets a short-array-valued annotation element. */
    fun put(field: String, value: ShortArray): AnnotationBuilder

    /** Sets an integer-array-valued annotation element. */
    fun put(field: String, value: IntArray): AnnotationBuilder

    /** Sets a long-array-valued annotation element. */
    fun put(field: String, value: LongArray): AnnotationBuilder

    /** Sets a float-array-valued annotation element. */
    fun put(field: String, value: FloatArray): AnnotationBuilder

    /** Sets a double-array-valued annotation element. */
    fun put(field: String, value: DoubleArray): AnnotationBuilder

    /** Sets a string-array-valued annotation element. */
    fun put(field: String, value: Array<String>): AnnotationBuilder

    /** Sets a boolean-array-valued annotation element. */
    fun put(field: String, value: BooleanArray): AnnotationBuilder

    /** Sets a class-array-valued annotation element. */
    fun put(field: String, value: Array<ClassType>): AnnotationBuilder
}

/**
 * An [AnnotationBuilder] that can produce the completed annotation model.
 */
interface CompletableAnnotationBuilder : AnnotationBuilder {

    /**
     * Completes the annotation definition.
     *
     * @return the resulting annotation declaration.
     */
    fun build(): KvmAnnotation
}


/**
 * Builder for defining a JVM class.
 *
 * A class builder can configure the class's access flags, superclass,
 * implemented interfaces, annotations, fields, methods, constructors,
 * and static initializer.
 *
 * The default superclass is `java.lang.Object`.
 */
interface ClassBuilder : Annotateable {

    /**
     * The access flags of the generated class.
     */
    var flags: ClassFlags

    /**
     * The superclass of the generated class.
     *
     * Defaults to `java.lang.Object`.
     */
    var superClass: ClassType

    /**
     * The interfaces implemented by the generated class.
     */
    var interfaces: List<ClassType>

    /**
     * Attaches an annotation to the generated class.
     *
     * @param name the fully qualified name of the annotation type.
     * @return a builder used to configure the annotation's element values.
     */
    override fun annotation(name: String): AnnotationBuilder

    /**
     * Creates a method builder for a method declared by this class.
     *
     * @param name the name of the method.
     * @param returnType the method's return type. Defaults to [VoidType].
     * @return a builder for configuring the method.
     */
    fun method(name: String, returnType: Type = VoidType): MethodBuilder

    /**
     * Creates a builder for a constructor of this class.
     *
     * Constructors are represented by the JVM method name `<init>` and have
     * a void return type.
     *
     * @return a builder for configuring the constructor.
     */
    fun constructor(): MethodBuilder

    /**
     * Creates a builder for the class's static initializer.
     *
     * The generated method corresponds to the JVM `<clinit>` method.
     *
     * @return the control-flow and value builders used to define the
     * static initializer.
     */
    fun static(): Pair<ControlFlowBuilder, ValueBuilder>

    /**
     * Adds a field declaration to the class.
     *
     * @param name the field name.
     * @param type the field's type.
     * @param accessFlags the field's access flags.
     * @return a builder for configuring the field.
     */
    fun field(
        name: String,
        type: Type,
        accessFlags: FieldFlags = FieldFlags.EMPTY
    ): FieldBuilder
}

/**
 * A [ClassBuilder] that can produce the completed class declaration.
 */
interface CompletableClassBuilder : ClassBuilder {

    /**
     * Completes the class definition.
     *
     * @return the resulting class declaration.
     */
    fun build(): ConcreteClass
}

/**
 * Builder for configuring a field declaration.
 *
 * Fields can be annotated through [annotation].
 */
interface FieldBuilder : Annotateable {

    /**
     * Attaches an annotation to the field.
     *
     * @param name the fully qualified name of the annotation type.
     * @return a builder for configuring the annotation.
     */
    override fun annotation(name: String): AnnotationBuilder
}

/**
 * A [FieldBuilder] that can produce the completed field declaration.
 */
interface CompletableFieldBuilder : FieldBuilder {

    /**
     * Completes the field definition.
     *
     * @return the resulting field declaration.
     */
    fun build(): ConcreteField
}

/**
 * Builder for defining a method's signature, flags, annotations,
 * parameters, and implementation.
 *
 * A method body can be created with [body]. Methods without a body can be
 * used for declarations such as abstract or native methods.
 */
interface MethodBuilder : Annotateable {

    /**
     * The access flags of this method.
     */
    var flags: MethodFlags

    /**
     * Attaches an annotation to the generated method.
     *
     * @param name the fully qualified name of the annotation type.
     * @return a builder for configuring the annotation.
     */
    override fun annotation(name: String): AnnotationBuilder

    /**
     * Creates a builder for defining this method's parameters.
     *
     * @return a parameter builder.
     */
    fun parameters(): ParametersBuilder

    /**
     * Creates the builders used to define the method body.
     *
     * The [ControlFlowBuilder] is responsible for control-flow constructs
     * and statements, while the [ValueBuilder] creates values referenced
     * by those statements.
     *
     * @return the control-flow and value builders for this method.
     */
    fun body(): Pair<ControlFlowBuilder, ValueBuilder>
}

/**
 * A [MethodBuilder] that can produce the completed method declaration.
 */
interface CompletableMethodBuilder : MethodBuilder {

    /**
     * Completes the method definition.
     *
     * @return the resulting method declaration.
     */
    fun build(): PresentMethodDeclaration
}

/**
 * Builder for defining the parameters of a method.
 */
interface ParametersBuilder {

    /**
     * Adds a named parameter to the method.
     *
     * @param name the parameter name used by the bytecode DSL.
     * @param type the parameter's type.
     * @return this builder.
     */
    fun parameter(name: String, type: Type): ParametersBuilder
}

/**
 * Builder for defining the control flow and statements of a method body.
 *
 * The builder provides structured operations for labels, branches, switches,
 * exception handling, returns, field/local storage, constructor calls, and
 * direct JVM instructions.
 */
interface ControlFlowBuilder {

    /**
     * Associates the following bytecode with a source line number.
     *
     * @param n the source line number.
     * @return this builder.
     */
    fun line(n: Int): ControlFlowBuilder

    /**
     * Places a label at the current position.
     *
     * @param label the label to place.
     * @return this builder.
     */
    fun placeLabel(label: Label): ControlFlowBuilder

    /**
     * Emits an unconditional jump to [label].
     *
     * @param label the destination label.
     * @return this builder.
     */
    fun goto(label: Label): ControlFlowBuilder

    /**
     * Begins a conditional branch.
     *
     * The returned scoped builder must be closed with either [BranchCompleter.otherwise]
     * or [BranchCompleter.complete].
     *
     * @param condition the value used as the branch condition.
     * @return a scoped builder for defining the branch body.
     */
    fun branch(condition: Value): ScopedControlFlowBuilder<BranchCompleter>

    /**
     * Evaluates a value as a statement.
     *
     * @param value the value to evaluate.
     * @param pop whether the resulting value should be removed from the operand
     * stack. Defaults to `true`.
     * @return this builder.
     */
    fun eval(value: Value, pop: Boolean = true): ControlFlowBuilder

    /**
     * Emits a return from the current method.
     *
     * @param value the value to return, or `null` for a void return.
     * @return this builder.
     */
    fun returns(value: Value? = null): ControlFlowBuilder

    /**
     * Throws the supplied throwable value.
     *
     * @param value the throwable to throw.
     * @return this builder.
     */
    fun throws(value: Value): ControlFlowBuilder

    /**
     * Stores a value through a pointer.
     *
     * @param ptr the destination pointer.
     * @param value the value to store.
     * @return this builder.
     */
    fun store(ptr: Ptr, value: Value): ControlFlowBuilder

    /**
     * Begins a switch statement.
     *
     * @param value the integer value used for dispatch.
     * @return a builder for defining switch cases.
     */
    fun switch(value: Value): SwitchBuilder

    /**
     * Begins a protected exception-handling region.
     *
     * @return a scoped builder for defining rescue and finally handlers.
     */
    fun attempt(): ScopedControlFlowBuilder<AttemptCompleter>

    /**
     * Emits a call to the superclass constructor.
     *
     * This operation is intended for constructors.
     *
     * @param arguments the arguments passed to the superclass constructor.
     * @return this builder.
     */
    fun superCall(vararg arguments: Value): ControlFlowBuilder

    /**
     * Emits a raw JVM instruction.
     *
     * This provides an escape hatch for instructions not represented by
     * the higher-level control-flow API.
     *
     * @param instruction the instruction to emit.
     * @return this builder.
     */
    fun instruction(instruction: Instruction): ControlFlowBuilder
}

/**
 * A scoped control-flow builder that temporarily redirects operations to a
 * nested control-flow region.
 *
 * @param R the object produced when the scope is closed.
 */
abstract class ScopedControlFlowBuilder<R>(
    flow: ControlFlowBuilder
) : ControlFlowBuilder by flow {

    /**
     * Closes this control-flow scope.
     *
     * @return the completer for the enclosing control-flow construct.
     */
    abstract fun close(): R
}


/**
 * Completes a conditional branch.
 */
interface BranchCompleter {

    /**
     * Begins the `else` branch.
     *
     * @return a scoped builder for the `else` body.
     */
    fun otherwise(): ScopedControlFlowBuilder<ControlFlowBuilder>

    /**
     * Completes the branch without an `else` body.
     *
     * @return the enclosing control-flow builder.
     */
    fun complete(): ControlFlowBuilder
}

/**
 * Builder for values referenced by a method body.
 *
 * Values form the expression layer of the bytecode DSL. They can represent
 * constants, parameters, locals, fields, arrays, method calls, object
 * construction, casts, and other JVM-level operations.
 */
interface ValueBuilder {

    companion object {

        /**
         * The JVM null value.
         */
        val NULL: Value = NullValue

        /**
         * A constant boolean value representing `true`.
         */
        val TRUE: Value = BooleanValue(true)

        /**
         * A constant boolean value representing `false`.
         */
        val FALSE: Value = BooleanValue(false)
    }

    /**
     * Creates a new label.
     *
     * @return a newly allocated label.
     */
    fun label(): Label

    /**
     * Resolves a method parameter by name.
     *
     * @param name the parameter name.
     * @return a pointer to the parameter's local variable.
     * @throws IllegalArgumentException if no parameter with the given name exists.
     */
    fun parameter(name: String): LocalPtr

    /**
     * Allocates a local variable of the specified type.
     *
     * @param type the local variable's type.
     * @return a pointer to the allocated local variable.
     */
    fun local(type: Type): LocalPtr

    /**
     * The receiver of the current instance method.
     *
     * Accessing `THIS` from a static method is invalid.
     */
    val THIS: Value
}

interface SwitchBuilder {

    /**
     * Begins a case for the specified integer key.
     *
     * @param key the integer case value.
     * @return a scoped builder for the case body.
     */
    fun case(key: Int): ScopedControlFlowBuilder<SwitchBuilder>

    /**
     * Begins the default case.
     *
     * @return a scoped builder for the default case body.
     */
    fun otherwise(): ScopedControlFlowBuilder<ControlFlowBuilder>

    /**
     * Completes the switch statement.
     *
     * @return the enclosing control-flow builder.
     */
    fun complete(): ControlFlowBuilder
}


/**
 * Completes a protected exception-handling region.
 */
interface AttemptCompleter {

    /**
     * Adds an exception handler for the specified exception type.
     *
     * @param type the exception type handled by the rescue block.
     * @return a scoped builder for the handler body.
     */
    fun rescue(type: ClassType): ScopedControlFlowBuilder<AttemptCompleter>

    /**
     * Adds a `finally`-style handler.
     *
     * @return a scoped builder for the handler body.
     */
    fun finally(): ScopedControlFlowBuilder<ControlFlowBuilder>

    /**
     * Completes the exception-handling construct.
     *
     * @return the enclosing control-flow builder.
     */
    fun complete(): ControlFlowBuilder
}
