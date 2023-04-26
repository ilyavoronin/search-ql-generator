package generator.parser

import generator.exec.graph.ExecutionGraph
import generator.lang.parser.getLangParser
import generator.scheme.GeneratorScheme
import generator.scheme.parser.astParser
import parser.inp
import utils.getResourceAsText
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExecutionGraphTest {

    val scheme: GeneratorScheme
    init {
        val schemeStr = getResourceAsText("teamcity.gs")!!
        scheme = GeneratorScheme(astParser.parse(schemeStr.inp()).unwrap())
    }

    @Test
    fun testBuildGraph() {
        val input = """
            find trigger
            in
            	project
            	(
            		name ("abacaba")
            		and
            		(build_conf (name ("T1")) or not build_conf (name ("T2")) )
            	) -> {
            		build_conf
            		(
            		   name ("abacaba")
            		) and
            		template
            		(
            		   id ("qwerty")
            		)
            	}.{type ("1")}

            	or 

            	project 
            	(
            	   name ("aba2")
            	).{type ("2")}
            
            
            with type ("scheduled")
        """.trimIndent()

        val p = getLangParser(scheme)
        val res = p.parse(input.inp()).unwrap()

        val graph = ExecutionGraph(scheme, res)

        var commonTriggerNode: ExecutionGraph.ExecutionNode

        assertEquals(38, graph.idToNode.size)
        assertNode(37, ExecutionGraph.PathOrExecutionNode::class, graph.root, 2)
        run {
            val node = assertNode(25, ExecutionGraph.PathSubExecNode::class, graph.root.getChildren()[0], 2)
            assertEquals("Project", node.obj.name)
            run {
                val node = assertNode(15, ExecutionGraph.ObjAndExecNode::class, node.getChildren()[0], 2)
                run {
                    val node = assertNode(6, ExecutionGraph.ObjSubObjExecNode::class, node.getChildren()[0], 1)

                    assertEquals("Name", node.obj.name)

                    assertNode(5, ExecutionGraph.ObjStringExecNode::class, node.getChildren()[0], 0)
                }
                run {
                    val node = assertNode(14, ExecutionGraph.ObjOrExecNode::class, node.getChildren()[1], 2)
                    run {
                        val node = assertNode(9, ExecutionGraph.ObjSubObjExecNode::class, node.getChildren()[0], 1)
                        assertEquals("BuildConf", node.obj.name)

                        run {
                            val node = assertNode(8, ExecutionGraph.ObjSubObjExecNode::class, node.getChildren()[0], 1)
                            assertEquals("Name", node.obj.name)

                            assertNode(7, ExecutionGraph.ObjStringExecNode::class, node.getChildren()[0], 0)
                        }
                    }
                    run {
                        val node = assertNode(13, ExecutionGraph.ObjNotExecNode::class, node.getChildren()[1], 1)
                        run {
                            val node = assertNode(12, ExecutionGraph.ObjSubObjExecNode::class, node.getChildren()[0], 1)
                            assertEquals("BuildConf", node.obj.name)

                            run {
                                val node = assertNode(11, ExecutionGraph.ObjSubObjExecNode::class, node.getChildren()[0], 1)
                                assertEquals("Name", node.obj.name)

                                assertNode(10, ExecutionGraph.ObjStringExecNode::class, node.getChildren()[0], 0)
                            }
                        }
                    }
                }
            }

            run {
                val node = assertNode(24, ExecutionGraph.PathAndExecutionNode::class, node.getChildren()[1], 2)

                run {
                    val node = assertNode(19, ExecutionGraph.PathSubExecNode::class, node.getChildren()[0], 2)
                    assertEquals("BuildConf", node.obj.name)

                    run {
                        val node = assertNode(17, ExecutionGraph.ObjSubObjExecNode::class, node.getChildren()[0], 1)
                        assertEquals("Name", node.obj.name)
                        assertNode(16, ExecutionGraph.ObjStringExecNode::class, node.getChildren()[0], 0)

                    }
                    run {
                        val node = assertNode(18, ExecutionGraph.PathSubExecNode::class, node.getChildren()[1], 1)
                        assertEquals("Trigger", node.obj.name)

                        run {
                            val node = assertNode(4, ExecutionGraph.ObjAndExecNode::class, node.getChildren()[0], 2)
                            run {
                                val node =
                                    assertNode(3, ExecutionGraph.ObjSubObjExecNode::class, node.getChildren()[0], 1)
                                assertEquals("Type", node.obj.name)
                                assertNode(2, ExecutionGraph.ObjStringExecNode::class, node.getChildren()[0], 0)
                            }

                            assertEquals(1, node.getChildren()[1].id)
                            commonTriggerNode = node.getChildren()[1]
                        }
                    }
                }

                run {
                    val node = assertNode(23, ExecutionGraph.PathSubExecNode::class, node.getChildren()[1], 2)
                    assertEquals("Template", node.obj.name)

                    run {
                        val node = assertNode(21, ExecutionGraph.ObjSubObjExecNode::class, node.getChildren()[0], 1)
                        assertEquals("Id", node.obj.name)
                        assertNode(20, ExecutionGraph.ObjStringExecNode::class, node.getChildren()[0], 0)

                    }
                    run {
                        val node = assertNode(22, ExecutionGraph.PathSubExecNode::class, node.getChildren()[1], 1)
                        assertEquals("Trigger", node.obj.name)

                        assertEquals(4, node.getChildren()[0].id)
                    }
                }
            }
        }

        run {
            val node = assertNode(36, ExecutionGraph.PathSubExecNode::class, graph.root.getChildren()[1], 2)
            assertEquals("Project", node.obj.name)

            run {
                val node = assertNode(30, ExecutionGraph.ObjSubObjExecNode::class, node.getChildren()[0], 1)
                assertEquals("Name", node.obj.name)

                assertNode(29, ExecutionGraph.ObjStringExecNode::class, node.getChildren()[0], 0)
            }
            run {
                val node = assertNode(35, ExecutionGraph.PathOrExecutionNode::class, node.getChildren()[1], 2)

                run {
                    val node = assertNode(32, ExecutionGraph.PathSubExecNode::class, node.getChildren()[0], 1)
                    assertEquals("BuildConf", node.obj.name)


                    run {
                        val node = assertNode(31, ExecutionGraph.PathSubExecNode::class, node.getChildren()[0], 1)
                        assertEquals("Trigger", node.obj.name)

                        assertEquals(28, node.getChildren()[0].id)
                    }
                }

                run {
                    val node = assertNode(34, ExecutionGraph.PathSubExecNode::class, node.getChildren()[1], 1)
                    assertEquals("Template", node.obj.name)


                    run {
                        val node = assertNode(33, ExecutionGraph.PathSubExecNode::class, node.getChildren()[0], 1)
                        assertEquals("Trigger", node.obj.name)

                        run {
                            val node = assertNode(28, ExecutionGraph.ObjAndExecNode::class, node.getChildren()[0], 2)

                            run {
                                val node = assertNode(27, ExecutionGraph.ObjSubObjExecNode::class, node.getChildren()[0], 1)
                                assertEquals("Type", node.obj.name)

                                assertNode(26, ExecutionGraph.ObjStringExecNode::class, node.getChildren()[0], 0)
                            }
                            assertEquals(commonTriggerNode.id, node.getChildren()[1].id)
                        }
                    }
                }
            }
        }

        val node = assertNode(1, ExecutionGraph.ObjSubObjExecNode::class, commonTriggerNode, 1)
        assertEquals("Type", node.obj.name)

        assertNode(0, ExecutionGraph.ObjStringExecNode::class, commonTriggerNode.getChildren()[0], 0)
    }
}

fun <T: Any> assertNode(id: Int, typ: KClass<T>, node: ExecutionGraph.ExecutionNode, cntChildren: Int): T {
    assertEquals(id, node.id)
    assertTrue(typ.isInstance(node))
    assertEquals(cntChildren, node.getChildren().size)

    return node as T
}