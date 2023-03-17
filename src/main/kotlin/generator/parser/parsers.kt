package generator.parser

import generator.ast.*
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

    Modifier(name)
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
    spaces[it]
    val mods = parseList(parseVar, "(", ")", ",")(it)
    newLine[it]
    DefField(
        name,
        type,
        params.unwrapOrNull() ?: listOf(),
        mods.unwrapOrNull() ?: listOf()
    )
}