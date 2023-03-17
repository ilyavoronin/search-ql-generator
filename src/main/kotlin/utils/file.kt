package utils

fun getResourceAsText(path: String): String? {
    return object {}.javaClass.classLoader.getResource(path)?.readText()
}