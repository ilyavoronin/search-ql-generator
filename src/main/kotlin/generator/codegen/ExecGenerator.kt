package generator.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import generator.scheme.GeneratorScheme
import generator.scheme.ast.*
import java.nio.file.Files
import java.nio.file.Paths

internal object ExecGenerator {
    fun genCode(path: String, pack: String, scheme: GeneratorScheme) {
        if (!Files.exists(Paths.get(path))) {
            Files.createDirectory(Paths.get(path))
        }

        fun saveFileWithPackage(resourcePath: String, filename: String, vararg imports: String) {
            saveFile(path, resourcePath, filename, pack)
        }

        val sourceInt = genSourceInterface(scheme)
        FileSpec.builder(pack, "ObjectsSource.kt")
            .addType(sourceInt)
            .build()
            .writeTo(Paths.get(path))

        val execEngine = genExecutionEngine(scheme)
        FileSpec.builder(pack, "ExecEngine.kt")
            .addType(execEngine)
            .build()
            .writeTo(Paths.get(path))

        saveFileWithPackage("exec", "ValueObject.kt")
        saveFileWithPackage("exec", "ExecType.kt")
        saveFileWithPackage("exec" ,"GeneratedObjects.kt")
        saveFileWithPackage("exec", "FixedBottomUpExecOrder.kt")
        saveFileWithPackage("exec", "ExecutionOrder.kt")
        saveFileWithPackage("exec", "ExecutionGraph.kt")
    }

    private fun genExecutionEngine(scheme: GeneratorScheme): TypeSpec {
        val res = TypeSpec.classBuilder("ExecutionEngine")
        val listClass = ClassName("kotlin.collections", "List")

        val astList: List<AST> = scheme.objects + scheme.filters + scheme.interfaces + scheme.modifiers

        fun genMembersList(m: List<DefField>): String {
            val l = m.joinToString(",") { def ->
                "DefField(${def.memName}, ${def.memType}, listOf(${def.modifiers.joinToString { it }}), ${def.isMany}, ${def.isSource}, ${def.isRev})"
            }
            return "listOf($l)"
        }
        val astStr = astList.mapNotNull { ast ->
            val res = when (ast) {
                is Object -> {
                    "Object(${ast.name}, ${ast.inheritedFrom}, ${genMembersList(ast.members)}, null, ${ast.source})"
                }
                is Filter -> {
                    "Filter(${ast.name}, ${ast.inheritedFrom}, ${genMembersList(ast.members)}, null)"
                }
                is Interface -> {
                    "Interface(${ast.name}, ${ast.inheritedFrom}, ${genMembersList(ast.members)})"
                }
                is Modifier -> {
                    null
                }
            }
            res
        }.joinToString(",")

        val constructor = FunSpec.constructorBuilder()
            .addParameter("source", TypeVariableName.invoke("ObjectsSource"))
            .addCode("""
                genObjects = GeneratedObjects(
                    listOf(${scheme.objects.joinToString(", ") { "${it.name}::class" }}),
                    listOf(${scheme.filters.joinToString(", ") { "${it.name}::class" }})
                )
                
                scheme = GeneratorScheme(listOf($astStr))
                parser = getLangParser(scheme)
            """.trimIndent())
            .build()

        res
            .addProperty("scheme", TypeVariableName.invoke("GeneratorScheme"), KModifier.PRIVATE)
            .addProperty("genObjects", TypeVariableName.invoke("ExecutionEngine"), KModifier.PRIVATE)
            .addProperty("parser", ClassName("", "Parser").parameterizedBy(TypeVariableName.invoke("FindQuery")))

        res.primaryConstructor(constructor)
            .addProperty(
                PropertySpec
                    .builder("source", TypeVariableName.invoke("ObjectsSource"))
                    .initializer("source")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

        res.addFunction(
            FunSpec.builder("execute")
                .addParameter("query", String::class)
                .returns(listClass.parameterizedBy(TypeVariableName.invoke("GeneratedObject")))
                .addCode(
                    """
                        val fquery = parser.parse(query).unwrap()
                        val execGraph = ExecutionGraph(scheme, genObjects, fquery)
                        return execGraph.execute(FixedBottomUpExecOrder())
                    """.trimIndent()
                )
                .build()
        )

        return res.build()
    }

    private fun genSourceInterface(scheme: GeneratorScheme): TypeSpec {
        val res = TypeSpec.interfaceBuilder("ObjectsSource")
        val listClass = ClassName("kotlin.collections", "List")


        scheme.objects.forEach { obj ->
            if (obj.source) {
                val getAll = FunSpec
                    .builder("getAll${obj.name}")
                    .addModifiers(KModifier.ABSTRACT)

                val retObject = TypeVariableName.invoke(obj.name)
                val parametrized = listClass.parameterizedBy(retObject)
                getAll.returns(parametrized)

                res.addFunction(getAll.build())
            }

            for (m in obj.members) {
                if (m.isSource) {
                    val getSourceProperty = FunSpec
                        .builder("get${obj.name}By${m.memName}")
                        .addModifiers(KModifier.ABSTRACT)

                    val retObject = TypeVariableName.invoke(m.memType)
                    val parameterized = listClass.parameterizedBy(retObject)
                    getSourceProperty.returns(parameterized)

                    res.addFunction(getSourceProperty.build())
                }
            }
        }

        return res.build()
    }
}