package generator.ast

sealed class AST

sealed class Definition(open val name: String): AST()

data class Modifier(val name: String): AST()

data class Object(
    override val name: String,
    val inheritedFrom: String?,
    val members: List<DefField>,
    val shortCut: ShortCut?,
    val source: Boolean
): Definition(name)

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
): Definition(name)

data class DefField(
    val memName: String,
    val memType: String,
    val params: List<String>,
    val modifiers: List<String>,
)

data class ShortCut(val parsingTemplate: String)
