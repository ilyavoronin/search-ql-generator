package generator.lang.parser

import generator.lang.ast.*
import generator.scheme.ExtendedDefField
import generator.scheme.GeneratorScheme
import generator.scheme.ast.*
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
        validateFindQuery(res, scheme)

        res
    }

    return findQuery.end()
}

private fun combine<*>.validateFindQuery(fquery: FindQuery, scheme: GeneratorScheme) {
    val sobj = scheme.getDefinition(fquery.sobject.capitalize()) ?: err("object type ${fquery.sobject.capitalize()} does not exist", 0)
    when (sobj) {
        is Filter -> {
            err("can't search ${sobj.name}, not an object", 0)
        }
        else -> {}
    }
    if (fquery.inCond != null) {
        validatePathCond(null,
            sobj,
            fquery.inCond,
            scheme
        )
    }
    if (fquery.withCond != null) {
        validateObjCond(sobj, fquery.withCond, scheme)
    }
}

private fun combine<*>.validatePathCond(obj: Definition?, sobj: Definition, cond: PathCondition, scheme: GeneratorScheme) {
    when (cond) {
        is AndObjPath -> {
            validatePathCond(obj, sobj, cond.l, scheme)
            validatePathCond(obj, sobj, cond.r, scheme)
        }
        is OrObjPath -> {
            validatePathCond(obj, sobj, cond.l, scheme)
            validatePathCond(obj, sobj, cond.r, scheme)
        }
        is SubObjPath -> {
            val (subObj, field) = if (obj == null) {
                Pair(
                    scheme.getDefinition(cond.objType.capitalize()) ?: err("unknown object type ${cond.objType}", 0),
                    null
                )
            } else {
                val subObjInfo = scheme.getSubObj(obj, cond.objType) ?: err("unknown subobject name ${cond.objType} for object ${obj.name}", 0)
                if (subObjInfo.second.reference) {
                    err("subobject of type ${cond.objType} is referenced by ${obj.name} so it cannot be used in path condition", 0)
                }
                subObjInfo
            }

            if (cond.modifiers.isNotEmpty()) {
                if (obj == null) {
                    err("toplevel object(${subObj.name} can't have modifiers", 0)
                }
                validateModifiers(obj, field!!, cond.modifiers, scheme)
            }

            if (cond.subObjPath != null) {
                validatePathCond(subObj, sobj, cond.subObjPath, scheme)
            } else {
                if (!scheme.checkHasAncestor(subObj, sobj)) {
                    err("${subObj.name} has no subobjects(transitive) of type ${sobj.name}", 0)
                }
            }

            if (cond.objCond != null) {
                validateObjCond(subObj, cond.objCond, scheme)
            }
            if (cond.addSearchObjCond != null) {
                validateObjCond(sobj, cond.addSearchObjCond, scheme)
            }
        }
    }
}

private fun combine<*>.validateObjCond(obj: Definition, cond: ObjCondition, scheme: GeneratorScheme) {
    when (cond) {
        is AndObjCond -> {
            validateObjCond(obj, cond.l, scheme)
            validateObjCond(obj, cond.r, scheme)
        }
        is OrObjCond -> {
            validateObjCond(obj, cond.l, scheme)
            validateObjCond(obj, cond.r, scheme)
        }
        is NotObjCond -> {
            validateObjCond(obj, cond.o, scheme)
        }
        is EmptyObjCond -> {
            if (!(obj.name == "bool" || obj.inheritedFrom?.first == "bool")) {
                err("${obj.name} is not bool type", 0)
            }
        }
        is IntObjectCond -> {
            if (!(obj.name == "int" || obj.inheritedFrom?.first == "int")) {
                err("${obj.name} is not int type", 0)
            }
        }
        is StringObjCond -> {
            if (!(obj.name == "string" || obj.inheritedFrom?.first == "string")) {
                err("${obj.name} is not string type", 0)
            }
        }
        is SubObjSearch -> {

            val (subObj, field) = scheme.getSubObj(obj, cond.objType) ?: err("unknown subobject name ${cond.objType} for object ${obj.name}", 0)
            validateModifiers(obj, field, cond.modifiers, scheme)
            validateObjCond(subObj, cond.objCond, scheme)
        }
    }
}

private fun combine<*>.validateModifiers(obj: Definition, field: ExtendedDefField, mods: Map<String, ModValueType>, scheme: GeneratorScheme) {
    val allowed = field.modifiers.toSet()

    for ((name, v) in mods) {
        val mod = scheme.getModifier(name) ?: err("modifier '${name}' doesn't exist", 0)
        if (!allowed.contains(name)) {
            err("${obj.name} field ${field.memName} does not have '${name}' modifier", 0)
        }

        when (mod.type) {
            is ModValueType.Int -> {
                if (v !is ModValueType.Int) {
                    err("$name modifier should have int type", 0)
                }
            }
            is ModValueType.Bool -> {
                if (v !is ModValueType.Bool) {
                    err("$name modifier should have bool type", 0)
                }
            }
            is ModValueType.String -> {
                if (v !is ModValueType.String) {
                    err("$name should have string type", 0)
                }
            }
        }
    }
}

private val modifierParser =
    combine<Pair<String, ModValueType>>{
        val name = parseVar.b()[it]
        token("(").b()[it]
        token("\"")[it]
        val v = parseTokenWhile { it != '"' }[it]
        token("\"").b()[it]
        Pair(name, ModValueType.String(v))
    } /
    combine {
        val name = parseVar.b()[it]
        token("(").b()[it]
        val v = parseTokenWhile { it.isDigit() }.map { it.toInt() } [it]
        token(")").b()[it]
        Pair(name, ModValueType.Int(v))
    } /
    combine {
        token("!")[it]
        val name = parseVar.b()[it]
        Pair(name, ModValueType.Bool(false))
    } /
    combine {
        val name = parseVar.b()[it]
        Pair(name, ModValueType.Bool(true))
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
        val modifiers = parseList(modifierParser, "[", "]", ",").b()(it).unwrapOrNull()?.toMap() ?: mapOf()
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
        SubObjPath(subObjType, subObjCond, subGraphCond, additionalSobjCond, modifiers)
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
        val modifiers = parseList(modifierParser, "[", "]", ",").b()(it).unwrapOrNull()?.toMap() ?: mapOf()
        token("(").b()[it]
        val subObjCond = topParser[it]
        token(")").b()[it]
        SubObjSearch(subObjType, subObjCond, modifiers)
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