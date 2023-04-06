package generator.lang.ast

sealed interface AST

sealed interface ObjCondition: AST

data class SubObjSearch(val objType: String, val objCond: ObjCondition, val searchObjType: String?) : ObjCondition

data class AndObjCond(val l: ObjCondition, val r: ObjCondition) : ObjCondition

data class OrObjCond(val l: ObjCondition, val r: ObjCondition) : ObjCondition

data class NotObjCond(val o: ObjCondition) : ObjCondition

data class FindQuery(val sobject: String, val inCond: ObjCondition?, val withCond: ObjCondition?)