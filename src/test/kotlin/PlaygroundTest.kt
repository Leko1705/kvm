import com.leko.kvm.ConcreteMethod
import com.leko.kvm.bytecode.buildClass
import com.leko.kvm.bytecode.call
import com.leko.kvm.callgraph.callGraph
import com.leko.kvm.constructors
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.IntType
import com.leko.kvm.typing.VoidType
import org.junit.jupiter.api.Test


class PlaygroundTest {

    @Test
    fun test() {
        val clazz = buildClass("MyClass") {
            flags = PUBLIC + ABSTRACT

            static { }

            public[IntType].field("x")

            public.constructor {
                superCall()
                ClassType("MyClass").call("foo").eval()
            }

            public.static.final[VoidType].method("foo")() {
                ClassType("MyClass").call("foo").eval()
            }

        }

        val const = clazz.constructors.first()
        val foo = clazz.methods.filterIsInstance<ConcreteMethod>().first { it.signature.name == "foo" }

        val cg = clazz.callGraph()
        for (edge in cg.edges) {
            println(edge)
        }
        println()
    }

}