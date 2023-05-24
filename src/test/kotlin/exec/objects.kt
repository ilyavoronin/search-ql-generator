package exec

import generated.exec.objects.*
import java.lang.IllegalStateException
import kotlin.reflect.KClass

lateinit var objs: Objs

abstract class TestObject(c: KClass<*>, val id: Int, vararg subObjs: List<TestObject>) {
    init {
        if (objs.idTOObj.containsKey(id)) {
            throw IllegalStateException("id $id already exists")
        }
        objs.idTOObj[id] = this
        objs.objsByType.computeIfAbsent(c.simpleName!!) { mutableListOf() }.add(this)
        for (so in subObjs) {
           if (so.isNotEmpty()) {
               objs.addSubObjs(c, id, so)
           }
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is TestObject && other.id == id
    }

    override fun hashCode(): Int {
        return id
    }
}

class O1(id: Int, val objs2: List<O2>, val objs3: List<O3>, val objs4: List<O4>): Obj1, TestObject(O1::class, id, objs2, objs3, objs4) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun getObj2() = objs2

    override fun getObj3() = objs3

    override fun getObj4() = objs4
}

class O2(id: Int, val objs4: List<O4>,  val objs5: List<O5>, val objs6: List<O6>, val objs9: List<O9>): Obj2, TestObject(O2::class, id, objs4, objs5, objs6, objs9) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun getObj4() = objs4

    override fun getObj5() = objs5

    override fun getObj6() = objs6

    override fun getObj9() = objs9

    override fun parentObj1(): List<Obj1> {
        return objs.getParent(this, O1::class)
    }
}

class O3(id: Int, val objs7: List<O7>, val objs8: List<O8>): Obj3, TestObject(O3::class, id, objs7, objs8) {
    override fun getObj7() = objs7

    override fun getObj8() = objs8

    override fun parentObj1(): List<Obj1> {
        return objs.getParent(this, O1::class)
    }

    override fun equals(other: Any?): Boolean {
        return other is O3 && other.id == id
    }

    override fun hashCode(): Int {
        return id
    }
}

class O4(id: Int): Obj4, TestObject(O4::class, id) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun parentObj1(): List<Obj1> {
        return objs.getParent(this, O1::class)
    }

    override fun parentObj2(): List<Obj2> {
        return objs.getParent(this, O2::class)
    }
}

class O5(id: Int): Obj5, TestObject(O5::class, id) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun parentObj2(): List<Obj2> {
        return objs.getParent(this, O2::class)
    }
}

class O6(id: Int): Obj6, TestObject(O6::class, id) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun parentObj2(): List<Obj2> {
        return objs.getParent(this, O2::class)
    }
}

class O7(id: Int): Obj7, TestObject(O7::class, id) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun parentObj3(): List<Obj3> {
        return objs.getParent(this, O3::class)
    }

}

class O8(id: Int): Obj8, TestObject(O8::class, id) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun parentObj3(): List<Obj3> {
        return objs.getParent(this, O3::class)
    }
}

class O9(id: Int, val objs10: List<O10>): Obj9, TestObject(O9::class, id, objs10) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun getObj10() = objs10

    override fun parentObj2(): List<Obj2> {
        return objs.getParent(this, O2::class)
    }

}

class O10(id: Int, val objs11: List<O11>, val objs12: List<O12>): Obj10, TestObject(O10::class, id, objs11, objs12) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun getObj11() = objs11

    override fun getObj12() = objs12

    override fun parentObj9(): List<Obj9> {
        return objs.getParent(this, O9::class)
    }
}

class O11(id: Int): Obj11, TestObject(O11::class, id) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun parentObj10(): List<Obj10> {
        return objs.getParent(this, O10::class)
    }

}

class O12(id: Int): Obj12, TestObject(O12::class, id) {
    override fun getId(): Id {
        return id.wrap()
    }

    override fun parentObj10(): List<Obj10> {
        return objs.getParent(this, O10::class)
    }

}

private fun Int.wrap() = ID(this)

data class ID(val id: Int): Id {
    override fun getInt(): Int {
        return id
    }
}

class Objs {
    val parentObjs = mutableMapOf<String, MutableMap<Int, MutableList<Int>>>()
    val idTOObj = mutableMapOf<Int, TestObject>()
    val objsByType = mutableMapOf<String, MutableList<TestObject>>()

    var cntGetAllCalled = 0

    fun createObjs() {
        O1(0, listOf(), listOf(), listOf())
        O1(1, listOf(), listOf(), listOf())
        O1(2, listOf(), listOf(), listOf())

        val o41 = O4(7)
        O1(3,
            listOf(
                O2(4,
                    listOf(o41),
                    listOf(O5(5)),
                    listOf(O6(6)),
                    listOf()
                )
            ),
            listOf(), listOf(o41, O4(13))
        )

        O1(8,
            listOf(
                O2(9,
                    listOf(o41, O4(14)),
                    listOf(O5(11)),
                    listOf(O6(12)),
                    listOf()
                )
            ),
            listOf(), listOf()
        )

        O1(15, listOf(
            O2(16, listOf(), listOf(), listOf(), listOf(
                O9(17, listOf(O10(18, listOf(O11(19), O11(23), O11(24)), listOf(O12(20), O12(21), O12(22)))))
            ))
        ), listOf(), listOf())
    }

    fun addSubObjs(objc: KClass<*>, id: Int, subObjs: List<TestObject>) {
        if (subObjs.isNotEmpty()) {
            val h = subObjs[0]::class.simpleName + "|" + objc.simpleName
            for (sobj in subObjs) {
                val m = parentObjs.computeIfAbsent(h) { mutableMapOf() }
                m.computeIfAbsent(sobj.id) { mutableListOf()}.add(id)
            }
        }
    }

    fun <T2: TestObject> getParent(obj: TestObject, t2: KClass<T2>): List<T2> {
        val h = obj::class.simpleName + "|" + t2.simpleName
        return parentObjs.get(h)?.get(obj.id)?.let {
            it.map { idTOObj[it]!! } as List<T2>
        } ?: listOf()
    }

    fun <T: TestObject> getAllObjs(objc: KClass<T>): List<T> {
        cntGetAllCalled += 1
        return objsByType[objc.simpleName] as List<T>
    }
}