package generator.codegen

object SchemeGenerator {
    fun genCode(path: String, pack: String) {

        fun saveFileWithPackage(resourcePath: String, filename: String, vararg imports: String) {
            saveFile(path, resourcePath, filename, pack, *imports)
        }

        saveFileWithPackage("scheme", "GeneratorScheme.kt")
        saveFileWithPackage("scheme", "objects.kt")
    }
}