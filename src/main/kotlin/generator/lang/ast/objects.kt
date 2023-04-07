package generator.lang.ast

sealed interface AST

sealed interface ObjCondition: AST

sealed interface PathCondition: AST

data class SubObjSearch(val objType: String, val objCond: ObjCondition) : ObjCondition

data class AndObjCond(val l: ObjCondition, val r: ObjCondition) : ObjCondition

data class OrObjCond(val l: ObjCondition, val r: ObjCondition) : ObjCondition

data class NotObjCond(val o: ObjCondition) : ObjCondition

data class StringObjCond(val s: String) : ObjCondition

data class IntObjectCond(val i: Int) : ObjCondition

data class EmptyObjCond(private val unit: Unit = Unit): ObjCondition


data class SubObjPath(
    val objType: String,
    val objCond: ObjCondition?,
    val SubObjPath: PathCondition?,
    val addSearchObjCond: ObjCondition?
    ) : PathCondition

data class AndObjPath(val l: PathCondition, val r: PathCondition) : PathCondition

data class OrObjPath(val l: PathCondition, val r: PathCondition) : PathCondition

data class FindQuery(val sobject: String, val inCond: PathCondition?, val withCond: ObjCondition?)