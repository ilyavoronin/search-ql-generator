import utils.getResourceAsText

fun main() {
    generateCode(getResourceAsText("tests/exec.gs")!!, "src/test/kotlin", "generated.exec")
}