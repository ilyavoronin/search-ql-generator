package generator.ast

sealed class AST

sealed class Definition: AST() {
    abstract val name: String
    abstract val inheritedFrom: String?
    abstract val members: List<DefField>
}

sealed class Accessible: Definition() {
    abstract val shortCut: ShortCut?
}

data class Modifier(val name: String): AST()

data class Object(
    override val name: String,
    override val inheritedFrom: String?,
    override val members: List<DefField>,
    override val shortCut: ShortCut?,
    val source: Boolean
): Accessible()

data class Interface(
    override val name: String,
    override val inheritedFrom: String?,
    override val members: List<DefField>,
): Definition()

data class Filter(
    override val name: String,
    override val inheritedFrom: String?,
    override val members: List<DefField>,
    override val shortCut: ShortCut?,
): Accessible()

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
