package generator.lang.parser

import generator.lang.ast.*
import generator.scheme.GeneratorScheme
import parser.*

private val parseVar = parseTokenWhile { it.isLetterOrDigit() || it in listOf('_') }

fun getLangParser(scheme: GeneratorScheme): Parser<FindQuery> {
    val objCondParser = getLowLevelParser()
    val objPathParser = getUpperLevelParser(objCondParser)
    val findQuery = combine {
        token("find").b()[it]
        val sobj = parseVar.br()[it]
        val inPart = if (token("in").b()(it).isOk()) {
            objPathParser[it]
        } else {
            null
        }
        val withPart = if (token("with").b()(it).isOk()) {
            objCondParser[it]
        } else {
            null
        }

        if (inPart == null && withPart == null) {
            err("no constraints were specified", -1)
        }

        val res = FindQuery(sobj, inPart, withPart)
        validateFindQuery(res)

        res
    }

    return findQuery.end()
}

private fun combine<*>.validateFindQuery(fquery: FindQuery) {
    // TODO
}

private fun getSubGraphParser(topParser: Parser<PathCondition>, condParser: Parser<ObjCondition>): Parser<PathCondition> {
    val objCond = combine {
        token("(").b()[it]
        val objCond = topParser[it]
        token(")").b()[it]
        objCond
    } /
    combine {
        val subObjType = parseVar.b()[it]
        val subObjCond = combine {
            token("(").b()[it]
            val subObjCond = condParser[it]
            token(")").b()[it]
            subObjCond
        }(it).unwrapOrNull()

        val subGraphCond = combine {
            token("->").b()[it]
            token("{").b()[it]
            val subGraph = topParser[it]
            token("}").b()[it]
            subGraph
        }(it).unwrapOrNull()
        val additionalSobjCond = combine {
            token(".").b()[it]
            token("{").b()[it]
            val res = condParser[it]
            token("}").b()[it]
            res
        }(it).unwrapOrNull()
        SubObjPath(subObjType, subObjCond, subGraphCond, additionalSobjCond)
    }

    return objCond
}

private fun getUpperLevelParser(objCondParser: Parser<ObjCondition>): Parser<PathCondition> = combine {
    val subGraph = getSubGraphParser(this, objCondParser)

    var andExp: PathCondition = subGraph[it]
    val orExps = mutableListOf<PathCondition>()

    while (true) {
        val op = (token("and").br() / token("or").br())(it)
        if (op.isOk()) {
            val rExp = subGraph[it]

            when (op.unwrap()) {
                "and" -> {
                    andExp = AndObjPath(andExp, rExp)
                }

                "or" -> {
                    orExps.add(andExp)
                    andExp = rExp
                }
            }
        } else {
            orExps.add(andExp)
            break
        }
    }
    orExps.reduce { acc, objCond1 ->
        OrObjPath(acc, objCond1)
    }
}

private fun getSubExpParser(topParser: Parser<ObjCondition>): Parser<ObjCondition> {
    val objCond = combine {
        token("(").b()[it]
        val objCond = topParser[it]
        token(")").b()[it]
        objCond
    } /
    combine {
        token("not").br()[it]
        NotObjCond(topParser[it])
    } /
    combine {
        val subObjType = parseVar.b()[it]
        token("(").b()[it]
        val subObjCond = topParser[it]
        token(")").b()[it]
        SubObjSearch(subObjType, subObjCond)
    } /
    combine {
        val num = parseTokenWhile { it.isDigit() }[it]
        if (num.isEmpty()) {
            err("empty num", it.pos)
        }
        blank.many()[it]
        IntObjectCond(num.toInt())
    } /
    combine {
        token("\"")[it]
        val str = parseTokenWhile { it != '"' }[it]
        token("\"")[it]
        StringObjCond(str)
    } /
    combine {
        EmptyObjCond()
    }

    return objCond
}

private fun getLowLevelParser(): Parser<ObjCondition> = combine {

    val objCond = getSubExpParser(this)
    var andExp: ObjCondition = objCond[it]
    val orExps = mutableListOf<ObjCondition>()

    while (true) {
        val op = (token("and").br() / token("or").br())(it)
        if (op.isOk()) {
            val rExp = objCond[it]

            when (op.unwrap()) {
                "and" -> {
                    andExp = AndObjCond(andExp, rExp)
                }

                "or" -> {
                    orExps.add(andExp)
                    andExp = rExp
                }
            }
        } else {
            orExps.add(andExp)
            break
        }
    }
    orExps.reduce { acc, objCond1 ->
        OrObjCond(acc, objCond1)
    }
}