import com.leko.kvm.PresentMethodDeclaration
import com.leko.kvm.bytecode.buildClass
import com.leko.kvm.bytecode.bytes
import com.leko.kvm.bytecode.defaultParser
import com.leko.kvm.name
import com.leko.kvm.returnType
import com.leko.kvm.typing.ClassType
import com.leko.kvm.typing.VoidType
import org.junit.jupiter.api.Test


class BytecodeLoadingTest {

    @Test
    fun `back and forth conversion`() {
        val clazz = buildClass("MyClass") {
            static {  }
            public.constructor {}

            annotation["MyAnno"]("name" to "myValue").
            public[ClassType("Foo")].method("foo")() { }
        }

        val bytecode = clazz.bytes()
        val loaded = defaultParser().parse(bytecode)

        assert(loaded.methods.any { it.name == "<init>" && it.returnType == VoidType })
        assert(loaded.methods.any { it.name == "foo" && it.returnType == ClassType("Foo") })

        val foo = loaded.methods.first { it.name == "foo" } as PresentMethodDeclaration
        assert(foo.annotations.any { it.name == "MyAnno" }) { foo.annotations }
    }

}