
# KVM - Java Bytecode Analysis Framework for Kotlin

The KVM Framework provides a variety of utilities for generating, manipulating and analyzing
entire java projects on a high level.

This includes things like:
- Control Flow Graphs
- Call Graphs


While the current implementation is implemented for JVM it is designed to be
adaptable to Multiplatform projects in the future.


## Features

### Class Generation

Use the `buildClass` to generate your class either on a high level or on instruction level.

```kt
import com.leko.kvm.bytecode.*

val clazz: ConcreteClass = buildClass("MyClass") {
    flags = PUBLIC + ABSTRACT

    static { }

    public[IntType].field("x")

    public.constructor() {
        superCall()
        ClassType("MyClass").call("foo").eval()
        returns()
    }

    public.static.final[VoidType].method("foo")() {
        var x by local(IntType)
        whileLoop(x lt int(3)) {
            ifTrue((x % int(2)) eq int(3)) {
                // ...
            }
            x += int(1)
        }

        asm {
            ICONST_0
            ICONST_1
            IADD
            ISTORE(0)  // a = 0 + 1
        }

        returns()
    }

}
```


# Bytecode Analysis

Load a jar file for analysis.

```kt
import com.leko.kvm.*

val jarFile: JarFile = JVMJarFileLoader.load("path/to/jarFile.jar")
```

Load a `Project` consisting of multiple (dependent) jars.
This allows you to analyze multiple jars at once.

```kt
val project = Project { 
    jar(jarFile)  // add an already loaded jar
    
    loadJar("path/to/jarFile.jar")
    
    buildJar {  // build a virtual jar file
        addClass("MyClass") {  // see 'Class Generation'
            // ...
        }
    }
}
```

Then use the project for analysis

```kt

val fooMethod = project.presentMethods
    .filterIsInstance<ConcreteMethod>()
    .first { it.name == "foo" }

val methodBody: MethodBody = fooMethod.body

val instructions: List<Instruction> = methodBody.instructions

val cfg = fooMethod.cfg()  // Control Flow Graph

val callGraph: CallGraph = project.callGraph()

analyze(project) {
    // ... scope providing more analysis functionality
}
```
