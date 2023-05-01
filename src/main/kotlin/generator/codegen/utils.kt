package generator.codegen

import utils.getResourceAsText
import java.nio.file.Files
import java.nio.file.Paths

fun saveCodeToFile(filename: String, code: String, basePath: String, pack: String, vararg imports: String) {
    val packPath = pack.split(".").toTypedArray()

    val fullPath = Paths.get(basePath, *packPath)
    val dir = fullPath.toFile()
    if (!dir.exists()) {
        Files.createDirectories(fullPath)
    }
    val file = Paths.get(fullPath.toString(), filename).toFile()

    file.writeText("""
package $pack

${imports.joinToString("\n") { "import $it" }}

$code
    """.trimIndent())
}

fun saveFile(path: String, resourcePath: String, filename: String, pack: String, vararg imports: String) {
    val code = getResourceAsText(Paths.get(resourcePath, "$filename.gen").toString())!!

    saveCodeToFile(filename, code, path, pack, *imports)
}