package generator.scheme

import generator.scheme.ast.*
import java.lang.RuntimeException

class GeneratorScheme(astList: List<AST>) {
    val interfaces: List<Interface>
    val filters: List<Filter>
    val objects: List<Object>
    val modifiers: List<Modifier>

    private val objectTree: MutableMap<String, MutableList<Definition>> = mutableMapOf()
    private val parentObjectTree: MutableMap<String, MutableList<Definition>> = mutableMapOf()
    private val interfaceMap: HashMap<String, Interface> = HashMap()
    private val modifierMap: HashMap<String, Modifier> = HashMap()
    private val defs: HashMap<String, Definition> = HashMap()
    private val objsMap: HashMap<String, Accessible> = HashMap()
    init {
        val ints = mutableListOf<Interface>()
        val objs = mutableListOf<Object>()
        val fs = mutableListOf<Filter>()
        val mods = mutableListOf<Modifier>()

        defs["string"] = Filter("string", null, listOf(), null)
        defs["bool"] = Filter("bool", null, listOf(), null)
        defs["int"] = Filter("int", null, listOf(), null)

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

        val interfaceObjectMap = mutableMapOf<String, List<Definition>>()
        for (ast in astList) {
            when (ast) {
                is Accessible -> {
                    objectTree[ast.name] = ast.members.map {
                        defs[it.memType]!!
                    }.toMutableList()
                }
                is Interface -> {
                    interfaceObjectMap[ast.name] = ast.members.map {
                        defs[it.memType]!!
                    }.toList()
                }
                else -> {}
            }
        }

        for (ast in astList) {
            when (ast) {
                is Accessible -> {
                    if (ast.inheritedFrom != null && ast.inheritedFrom !in listOf("string", "bool", "int")) {
                        objectTree[ast.name]!!.addAll(interfaceObjectMap[ast.inheritedFrom] ?: throw RuntimeException(ast.inheritedFrom))
                    }
                    for (subObj in objectTree[ast.name]!!) {
                        parentObjectTree.computeIfAbsent(subObj.name) { mutableListOf() }.add(ast)
                    }
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

    fun getDefinition(name: String): Definition? {
        return defs[name]
    }

    fun getSubObj(obj: Definition, defName: String): Pair<Definition, ExtendedDefField>? {
        val members = if (obj.inheritedFrom == null) {
            obj.members
        } else {
            obj.members + getDefinition(obj.inheritedFrom!!)!!.members
        }
        for (m in members) {
            if (m.memName == defName) {
                return getDefinition(m.memType)?.let {
                    Pair(it, ExtendedDefField(obj, m))
                }
            }
        }
        return null
    }

    fun checkHasAncestor(obj: Definition, sobj: Definition): Boolean {
        val used = mutableSetOf<String>()
        return checkHasAncestorRec(obj.name, sobj.name, used)
    }

    private fun checkHasAncestorRec(obj: String, sobj: String, used: MutableSet<String>): Boolean {
        if (used.contains(obj)) {
            return false
        }
        if (obj == sobj) {
            return true
        }
        used.add(obj)
        if (!objectTree.containsKey(obj)) {
            return false
        }
        for (subObj in objectTree[obj]!!) {
            val res = checkHasAncestorRec(subObj.name, sobj, used)
            if (res) {
                return true
            }
        }
        return false
    }
}

class ExtendedDefField(
    val parent: Definition?,
    defField: DefField,
) {
    val memName: String
    val memType: String
    val modifiers: List<String>
    val isMany: Boolean
    val isRev: Boolean
    val isSource: Boolean

    init {
        memName = defField.memName
        memType = defField.memType
        modifiers = defField.modifiers
        isMany = defField.isMany
        isRev = defField.isRev
        isSource = defField.isSource
    }

    constructor() : this(null, DefField("", "", emptyList(), false, false, false))
}