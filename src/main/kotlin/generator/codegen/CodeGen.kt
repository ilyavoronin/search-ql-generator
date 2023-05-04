package generator.codegen

import generator.scheme.GeneratorScheme
import java.nio.file.Files
import java.nio.file.Paths

object CodeGen {
    fun genCode(path: String, pack: String, scheme: GeneratorScheme): String {
        if (!Files.exists(Paths.get(path))) {
            Files.createDirectories(Paths.get(path))
        }
        ObjectsGenerator.genCode(path, "$pack.objects", scheme, pack)
        ExecGenerator.genCode(path, "$pack.exec", scheme, pack)
        ParserGenerator.genCode(path, "$pack.parser", pack)
        SchemeGenerator.genCode(path, "$pack.scheme")

        fun saveFileWithPackage(filename: String) {
            saveFile(path, "", filename, pack)
        }

        saveFileWithPackage("GeneratedObject.kt")
        saveFileWithPackage("SearchableGeneratedObject.kt")

        return Paths.get(path).toAbsolutePath().toString()
    }
}