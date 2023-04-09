package generator.scheme.parser

import generator.scheme.ast.*
import parser.*

private val parseVar = parseTokenWhile { it.isLetterOrDigit() || it in listOf('_') }


private val objectParser = combine {
    blank.many()[it]
    val source = token("source").br()(it).isOk()
    token("object").br()[it]
    val (name, inherited, fields) = defBodyParser[it]

    Object(name, inherited.unwrapOrNull(), fields.first,  fields.second, source)
}.or(combine {
    blank.many()[it]
    val source = token("source").br()(it).isOk()
    token("object").br()[it]
    val name = parseVar.s()[it]
    token(":").s()[it]
    val inheritedFrom = parseVar.b()[it]

    Object(name, inheritedFrom, listOf(), null, source)
})

private val filterParser = combine {
    blank.many()[it]
    token("filter").br()[it]
    val (name, inherited, fields) = defBodyParser[it]

    Filter(name, inherited.unwrapOrNull(), fields.first,  fields.second)
}.or(combine {
    blank.many()[it]
    token("filter").br()[it]
    val name = parseVar.s()[it]
    token(":").s()[it]
    val inheritedFrom = parseVar.b()[it]

    Filter(name, inheritedFrom, listOf(), null)
})

private val interfaceParser = combine {
    blank.many()[it]
    token("interface").br()[it]
    val (name, inherited, fields) = defBodyParser[it]

    Interface(name, inherited.unwrapOrNull(), fields.first)
}

private val defParser: Parser<Definition> = objectParser.or(filterParser).or(interfaceParser)

private val modifierParser: Parser<Modifier> = combine {
    blank.many()[it]
    token("modifier").br()[it]
    val name = parseVar.b()[it]
    token(":").b()[it]
    val type = parseVar.b()[it]
    token("(").b()[it]
    val typeAndDefaultValue = when(type) {
        "bool" -> {
            ModValueType.Bool(token("true").or(token("false"))[it].toBoolean())
        }
        "string" -> {
            token("\"")[it]
            val str = parseTokenWhile { it != '"' }[it]
            token("\"")[it]
            ModValueType.String(str)
        }
        "int" -> {
            val resStr = parseTokenWhile { it.isDigit() }[it]
            ModValueType.Int(resStr.toInt())
        }

        else -> {err("Unexpected type $type", it.pos)}
    }
    blank.many()[it]
    token(")").b()[it]

    Modifier(name, typeAndDefaultValue)
}

val astParser: Parser<List<AST>> = (modifierParser.or(defParser)).many().end()


private val defBodyParser = combine {
    val name = parseVar.b()[it]
    val inheritedFrom = combine {
        token(":").b()[it]
        parseVar[it]
    }.blank()(it)
    token("{").nl()[it]
    val fields = fieldParser.atLeast(1)[it]
    blank.many()[it]
    token("}").b()[it]

    val sc = combine {
        token("`").s()[it]
        val sc = parseTokenWhile {
            it != '`'
        }[it]
        token("`").s()[it]
        sc
    }(it).unwrapOrNull()?.let { ShortCut(it) }

    Triple(name, inheritedFrom, Pair(fields, sc))
}

private val fieldParser = combine {
    val name = parseVar.s()[it]
    token(":").s()[it]
    val type = parseVar.s()[it]
    val params = parseList(parseVar, "[", "]", ",")(it)
    var isRev = false
    var isMany = false
    var isSource = false

    params.unwrapOrNull()?.forEach {par ->
        when  {
            par == "many" -> isMany = true
            par == "rev" -> isRev = true
            par == "source" -> isSource = true
            else -> err("unknown field param $it", it.pos)
        }
    }

    spaces[it]
    val mods = parseList(parseVar, "(", ")", ",")(it)
    newLine[it]

    DefField(
        name,
        type,
        mods.unwrapOrNull() ?: listOf(),
        isMany,
        isSource,
        isRev,
    )
}