package generator.scheme

import generator.scheme.ast.*

class GeneratorScheme(astList: List<AST>) {
    val interfaces: List<Interface>
    val filters: List<Filter>
    val objects: List<Object>
    val modifiers: List<Modifier>

    private val objectTree: MutableMap<String, List<Definition>> = mutableMapOf()
    private val parentObjectTree: MutableMap<String, MutableList<Definition>> = mutableMapOf()
    private val interfaceMap: HashMap<String, Interface> = HashMap()
    private val modifierMap: HashMap<String, Modifier> = HashMap()
    private val objsMap: HashMap<String, Accessible> = HashMap()
    init {
        val ints = mutableListOf<Interface>()
        val objs = mutableListOf<Object>()
        val fs = mutableListOf<Filter>()
        val mods = mutableListOf<Modifier>()

        val defs = mutableMapOf<String, Definition>()

        for (ast in astList) {
            when (ast) {
                is Object -> {
                    objs.add(ast)
                    objsMap[ast.name] = ast
                }
                is Filter -> {
                    fs.add(ast)
                    objsMap[ast.name] = ast
                }
                is Interface -> {
                    interfaceMap[ast.name] = ast
                    ints.add(ast)
                }
                is Modifier -> {
                    mods.add(ast)
                    modifierMap[ast.name] = ast
                }
            }
            when (ast) {
                is Definition -> {
                    defs[ast.name] = ast
                }
                else -> {}
            }
        }
        for (ast in astList) {
            when (ast) {
                is Definition -> {
                    objectTree[ast.name] = ast.members.mapNotNull {
                        if (it.memType in listOf("string", "int", "bool")) {
                            null
                        } else {
                            parentObjectTree.computeIfAbsent(it.memType) { mutableListOf() }.add(ast)
                            defs[it.memType]!!
                        }
                    }.toList()
                }
                else -> {}
            }
        }

        interfaces = ints
        filters = fs
        objects = objs
        modifiers = mods
    }

    fun getInterface(name: String): Interface? {
        return interfaceMap[name]
    }

    fun getObjOrFilter(name: String): Accessible? {
        return objsMap[name]
    }

    fun getModifier(name: String): Modifier? {
        return modifierMap[name]
    }
}