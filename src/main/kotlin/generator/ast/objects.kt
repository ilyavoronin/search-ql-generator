package generator.ast

sealed class AST

sealed class Definition(open val name: String): AST()

sealed class Accessible(name: String): Definition(name)

data class Modifier(val name: String): AST()

data class Object(
    override val name: String,
    val inheritedFrom: String?,
    val members: List<DefField>,
    val shortCut: ShortCut?,
    val source: Boolean
): Accessible(name)

data class Interface(
    override val name: String,
    val inheritedFrom: String?,
    val members: List<DefField>,
): Definition(name)

data class Filter(
    override val name: String,
    val inheritedFrom: String?,
    val members: List<DefField>,
    val shortCut: ShortCut?,
): Accessible(name)

data class DefField(
    val memName: String,
    val memType: String,
    val modifiers: List<String>,
    //params:
    val isMany: Boolean,
    val isSource: Boolean,
    val isRev: Boolean
)

data class ShortCut(val parsingTemplate: String)
