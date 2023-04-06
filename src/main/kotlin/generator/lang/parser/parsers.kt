package generator.lang.parser

import generator.lang.ast.*
import generator.scheme.GeneratorScheme
import parser.*

private val parseVar = parseTokenWhile { it.isLetterOrDigit() || it in listOf('_') }

fun getLangParser(scheme: GeneratorScheme): Parser<FindQuery> {
    val searchObject = combine {
        val objParser = token("?")
        for (objDef in scheme.objects) {
            objParser.or(token(objDef.name.toLowerCase()))
        }
        objParser.b()[it]
    }
    val findQuery = combine {
        token("find").b()[it]
        val sobj = searchObject.b()[it]
        val inPart = if (token("in").b()(it).isOk()) {
            getLevelParser(true, false)[it]
        } else {
            null
        }
        val withPart = if (token("with").b()(it).isOk()) {
            getLevelParser(false, true)[it]
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

    return findQuery
}

private fun combine<*>.validateFindQuery(fquery: FindQuery) {
    // TODO
}

private fun getSubExpParser(topParser: Parser<ObjCondition>, allowSearchObjSpec: Boolean): Parser<ObjCondition> {
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
        val sobjType = if (allowSearchObjSpec) {combine {
            token("|").b()[it]
            parseVar[it]
        }.b()(it).unwrapOrNull() } else {
            null
        }
        SubObjSearch(subObjType, subObjCond, sobjType)
    }

    return objCond
}

private fun getLevelParser(allowSearchObjSpec: Boolean, allowConditional: Boolean): Parser<ObjCondition> = combine {

    val objCond = getSubExpParser(getLevelParser(allowSearchObjSpec, true), allowSearchObjSpec)
    var andExp: ObjCondition = objCond[it]
    val orExps = mutableListOf<ObjCondition>()

    if (allowConditional) {
        while (true) {
            val op = (token("and") / token("or"))(it)
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
    } else {
        objCond[it]
    }
}