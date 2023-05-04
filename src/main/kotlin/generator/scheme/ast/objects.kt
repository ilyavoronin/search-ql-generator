package generator.scheme.ast

sealed class AST

sealed class Definition: AST() {
    abstract val name: String
    abstract val inheritedFrom: Pair<String, Boolean>?
    abstract val members: List<DefField>
}

sealed class Accessible: Definition() {
    abstract val shortCut: ShortCut?
}


sealed class ModValueType(val typeStr: kotlin.String) {
    fun getValue(): Any {
        return when (this) {
            is Bool -> {
                this.v
            }
            is String -> {
                this.v
            }
            is Int -> {
                this.v
            }
        }
    }
    class Bool(val v: Boolean): ModValueType("kotlin.Boolean")
    class Int(val v: kotlin.Int): ModValueType("kotlin.Int")
    class String(val v: kotlin.String): ModValueType("kotlin.String")
}
data class Modifier(val name: String, val type: ModValueType, val revAllowed: Boolean): AST()

data class Object(
    override val name: String,
    override val inheritedFrom: Pair<String, Boolean>?,
    override val members: List<DefField>,
    override val shortCut: ShortCut?,
    val source: Boolean
): Accessible()

data class Interface(
    override val name: String,
    override val inheritedFrom: Pair<String, Boolean>?,
    override val members: List<DefField>,
): Definition()

data class Filter(
    override val name: String,
    override val inheritedFrom: Pair<String, Boolean>?,
    override val members: List<DefField>,
    override val shortCut: ShortCut?,
): Accessible()

data class DefField(
    val reference: Boolean,
    val memName: String,
    val memType: String,
    val modifiers: List<String>,
    //params:
    val isMany: Boolean,
    val isSource: Boolean,
    val isRev: Boolean,
    val inherited: Boolean = false,
)

data class ShortCut(val parsingTemplate: String)
