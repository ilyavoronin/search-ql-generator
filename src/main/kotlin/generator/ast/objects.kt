package generator.ast

sealed class Definition(open val name: String)

data class Object(
    override val name: String,
    val inheritedFrom: String?,
    val members: List<DefField>,
    val shortCut: ShortCut,
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
    val shortCut: ShortCut,
): Definition(name)

data class DefField(
    val memName: String,
    val memType: String,
    val modifiers: List<String>,
)

data class ShortCut(val parsingTemplate: String)
