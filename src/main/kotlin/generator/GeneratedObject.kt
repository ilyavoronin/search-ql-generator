package generator

interface GeneratedObject {
}

interface SearchableGeneratedObject: GeneratedObject {
    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}