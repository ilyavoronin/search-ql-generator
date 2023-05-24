package exec

import generated.exec.exec.ExecutionEngine
import generated.exec.exec.FixedBottomUpExecOrder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExecutionTest {

    val engine: ExecutionEngine
    init {
        engine = ExecutionEngine(ObjSourceImpl(), FixedBottomUpExecOrder())
    }

    @BeforeTest
    fun beforeTest() {
        objs = Objs()
        objs.createObjs()
    }

    // cond == null && subPath == null

    @Test
    fun testSubPathNodeCase1Num1() {
        val res = engine.execute("""
            find id in obj1 ( id(1) ) -> { id }
        """.trimIndent()) as List<ID>

        assertEquals(1, res.size)
        assertEquals(1, res[0].id)
        assertEquals(1, objs.cntGetAllCalled)
    }

    @Test
    fun testSubPathNodeCase1Num2() {
        val res = engine.execute("""
            find id in obj1 ( obj2( obj5( id(5) ) ) ) -> { id }
        """.trimIndent()) as List<ID>

        assertEquals(setOf(3), res.map { it.id }.toSet())
        assertEquals(0, objs.cntGetAllCalled)
    }

    @Test
    fun testSubPathNodeCase1Num3() {
        val res = engine.execute("""
            find obj4 in obj1 ( obj2( obj5( id(5) ) ) ) -> {obj4} and obj2( obj6( id(12) ) )
        """.trimIndent()) as List<O4>

        assertEquals(setOf(7), res.map { it.id }.toSet())
        assertEquals(0, objs.cntGetAllCalled)
    }

    @Test
    fun testSubPathNodeCase1Num4() {
        val res = engine.execute("""
            find obj4 in obj1 ( obj2( obj5( id(5) ) ) ) -> {obj4} or obj2( obj6( id(12) ) )
        """.trimIndent()) as List<O4>

        assertEquals(setOf(7, 13, 14), res.map { it.id }.toSet())
        assertEquals(1, objs.cntGetAllCalled)
    }

    // cond != null && subPath == null
    @Test
    fun testSubPathNodeCase2Num1() {
        val res = engine.execute("""
            find obj4 in obj1 ( obj2( obj5( id(5) ) ) ) -> {obj4} and obj2( obj6( id(12) ) )
            with id(7)
        """.trimIndent()) as List<O4>

        assertEquals(setOf(7), res.map { it.id }.toSet())
        assertEquals(0, objs.cntGetAllCalled)
    }

    @Test
    fun testSubPathNodeCase2Num2() {
        val res = engine.execute("""
            find obj4 in obj2
            with (id(14) or id(7) or id(13))
        """.trimIndent()) as List<O4>

        assertEquals(setOf(7, 14), res.map { it.id }.toSet())
        assertEquals(1, objs.cntGetAllCalled)
    }

    @Test
    fun testSubPathNodeCase2Num3() {
        val res = engine.execute("""
            find obj4 in obj1 ( obj2( obj5( id(5) ) ) ) -> {obj4} or obj2( obj6( id(12) ) )
            with id(7) or id(13)
        """.trimIndent()) as List<O4>

        assertEquals(setOf(7, 13), res.map { it.id }.toSet())
        assertEquals(1, objs.cntGetAllCalled)
    }


    // cond == null && subPath != null

    @Test
    fun testSubPathCase3Num1() {
        val res = engine.execute("""
            find obj12 in obj1
        """.trimIndent()) as List<O12>

        assertEquals(setOf(20, 21, 22), res.map { it.id }.toSet())
        assertEquals(1, objs.cntGetAllCalled)
    }

    @Test
    fun testSubPathCase3Num2() {
        val res = engine.execute("""
            find obj12 in obj1
            with id(20) or id(22)
        """.trimIndent()) as List<O12>

        assertEquals(setOf(20, 22), res.map { it.id }.toSet())
        assertEquals(1, objs.cntGetAllCalled)
    }

    @Test
    fun testSubPathCase3Num3() {
        val res = engine.execute("""
            find obj11 in obj1
            with id(19) or id(20) or id(24)
        """.trimIndent()) as List<O11>

        assertEquals(setOf(19, 24), res.map { it.id }.toSet())
        assertEquals(0, objs.cntGetAllCalled)
    }


    // cond == null && subPath != null

    @Test
    fun testSubPathCase4Num1() {
        val res = engine.execute("""
            find obj11 in obj1( obj2( id(16) ) )
            with id(19) or id(20) or id(24)
        """.trimIndent()) as List<O11>

        assertEquals(setOf(19, 24), res.map { it.id }.toSet())
        assertEquals(0, objs.cntGetAllCalled)
    }
}