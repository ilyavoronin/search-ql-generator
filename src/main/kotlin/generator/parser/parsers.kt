package generator.parser

import generator.ast.DefField
import generator.ast.Definition
import generator.ast.Object
import generator.ast.ShortCut
import parser.*

val parseVar = parseTokenWhile { it.isLetterOrDigit() }


private val objectParser = combine {
    val source = token("source").br()(it).isOk()
    token("object").br()[it]
    val (name, inherited, fields) = defBodyParser[it]

    Object(name, inherited.unwrapOrNull(), fields.first,  fields.second)
}

private val filterParser = combine {
    token("filter").br()[it]
    val (name, inherited, fields) = defBodyParser[it]

    Object(name, inherited.unwrapOrNull(), fields.first,  fields.second)
}

private val interfaceParser = combine {
    token("interface").br()[it]
    val (name, inherited, fields) = defBodyParser[it]

    Object(name, inherited.unwrapOrNull(), fields.first,  fields.second)
}

private val defParser: Parser<Definition> = objectParser.or(filterParser).or(interfaceParser)


private val defBodyParser = combine {
    val name = parseVar[it]
    val inheritedFrom = combine {
        token(":").b()[it]
        parseVar[it]
    }.blank()(it)
    token("{").nl()[it]
    val fields = fieldParser.atLeast(1)[it]
    token("}").b()[it]

    token("`").s()[it]
    val sc = parseTokenWhile {
        it != '`'
    }[it]
    token("`").s()[it]

    Triple(name, inheritedFrom, Pair(fields, ShortCut(sc)))
}

private val fieldParser = combine {
    val name = parseVar.s()[it]
    token(":").s()[it]
    val type = parseVar.s()[it]
    val mods = parseList(parseVar, "[", "]", ",").nl()[it]
    DefField(
        name,
        type,
        mods
    )
}