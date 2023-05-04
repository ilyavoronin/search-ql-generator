package generator.codegen

import com.squareup.kotlinpoet.FileSpec
import utils.getResourceAsText
import java.nio.file.Paths

internal object ParserGenerator {
    fun genCode(path: String, pack: String, basePack: String) {
        fun saveFileWithPackage(resourcePath: String, filename: String, pack: String, vararg imports: String) {
            saveFile(path, resourcePath, filename, pack, *imports)
        }

        val utilsPack = "$pack.utils"
        val langPack = "$pack.lang"

        saveFileWithPackage("parser", "combinators.kt", utilsPack)
        saveFileWithPackage("parser", "default.kt", utilsPack)
        saveFileWithPackage("parser", "Parser.kt", utilsPack)
        saveFileWithPackage("parser", "Result.kt", utilsPack)

        saveFileWithPackage("lang", "objects.kt", langPack,
            joinPackages(basePack, "scheme", "ModValueType")
        )
        saveFileWithPackage("lang", "parsers.kt", langPack,
            joinPackages(utilsPack, "*"),
            joinPackages(basePack, "scheme", "*")
        )
    }
}