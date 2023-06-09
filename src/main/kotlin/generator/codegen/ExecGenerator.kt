package generator.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import generator.scheme.GeneratorScheme
import generator.scheme.ast.*
import java.nio.file.Files
import java.nio.file.Paths

internal object ExecGenerator {
    fun genCode(path: String, pack: String, scheme: GeneratorScheme, basePack: String) {
        if (!Files.exists(Paths.get(path))) {
            Files.createDirectory(Paths.get(path))
        }

        fun saveFileWithPackage(resourcePath: String, filename: String, vararg imports: String) {
            saveFile(path, resourcePath, filename, pack, *imports)
        }

        val sourceInt = genSourceInterface(scheme, basePack)
        FileSpec.builder(pack, "ObjectsSource.kt")
            .addType(sourceInt)
            .build()
            .writeTo(Paths.get(path))

        val execEngine = genExecutionEngine(scheme, basePack)
        FileSpec.builder(pack, "ExecEngine.kt")
            .addImport(joinPackages(basePack, "scheme"), "GeneratorScheme", "Object", "Filter", "Interface", "Modifier", "DefField", "ModValueType")
            .addImport(joinPackages(basePack, "parser", "lang"), "FindQuery", "getLangParser")
            .addImport(joinPackages(basePack, "parser", "utils"), "inp")
            .addImport(basePack, "GeneratedObject")
            .addType(execEngine)
            .build()
            .writeTo(Paths.get(path))

        saveFileWithPackage("exec", "ValueObject.kt")

        saveFileWithPackage("exec", "ExecType.kt")

        saveFileWithPackage("exec" ,"GeneratedObjects.kt",
            joinPackages(basePack, "GeneratedObject"),
            joinPackages(basePack, "scheme", "ExtendedDefField"),
            joinPackages(basePack, "scheme", "GeneratorScheme"),
            joinPackages(basePack, "scheme", "ModValueType")
        )

        saveFileWithPackage("exec", "FixedBottomUpExecOrder.kt",
            joinPackages(basePack, "scheme", "Definition"),
            joinPackages(basePack, "scheme", "GeneratorScheme")
        )


        saveFileWithPackage("exec", "ExecutionOrder.kt",
            joinPackages(basePack, "scheme", "Definition"),
            joinPackages(basePack, "scheme", "GeneratorScheme")
        )

        saveFileWithPackage("exec", "ExecutionGraph.kt",
            "java.lang.IllegalStateException",
            joinPackages(basePack, "GeneratedObject"),
            joinPackages(basePack, "parser", "lang", "FindQuery"),
            joinPackages(basePack, "scheme", "*"),
            joinPackages(basePack, "objects", "StringBuiltIn"),
            joinPackages(basePack, "objects", "IntBuiltIn"),
            joinPackages(basePack, "objects", "BoolBuiltIn"),
            joinPackages(basePack, "parser", "lang", "*"),
            "kotlin.reflect.KCallable"
        )
    }

    private fun genExecutionEngine(scheme: GeneratorScheme, pack: String): TypeSpec {
        val res = TypeSpec.classBuilder("ExecutionEngine")
        val listClass = ClassName("kotlin.collections", "List")

        val astList: List<AST> = scheme.objects + scheme.filters + scheme.interfaces + scheme.modifiers

        fun genMembersList(m: List<DefField>): String {
            val l = m.joinToString(",") { def ->
                "DefField(${def.reference}, \"${def.memName}\", \"${def.memType}\", listOf(${def.modifiers.joinToString { "\"$it\"" }}), ${def.isMany}, ${def.isSource}, ${def.isRev})"
            }
            return "listOf($l)"
        }
        val astStr = astList.mapNotNull { ast ->
            val res = when (ast) {
                is Object -> {
                    val inhertiedString = ast.inheritedFrom?.let {
                        if (it.first in listOf("string", "int", "bool")) {
                            "Pair(\"${it.first}\", ${it.second})"
                        } else {
                            "null"
                        }
                    } ?: "null"
                    "Object(\"${ast.name}\", $inhertiedString, ${genMembersList(ast.members)}, null, ${ast.source})"
                }
                is Filter -> {
                    val inhertiedString = ast.inheritedFrom?.let {
                        if (it.first in listOf("string", "int", "bool")) {
                            "Pair(\"${it.first}\", ${it.second})"
                        } else {
                            "null"
                        }
                    } ?: "null"
                    "Filter(\"${ast.name}\", $inhertiedString, ${genMembersList(ast.members)}, null)"
                }
                is Interface -> {
                    null
                }
                is Modifier -> {
                    val t = ast.type
                    "Modifier(\"${ast.name}\", ${
                        when(t) {
                            is ModValueType.Bool -> "ModValueType.Bool(${t.v})"
                            is ModValueType.Int -> "ModValueType.Int(${t.v})"
                            is ModValueType.String -> "ModValueType.String(\"${t.v}\")"
                        }
                    }, ${ast.revAllowed})"
                }
            }
            res
        }.joinToString(",")

        val constructor = FunSpec.constructorBuilder()
            .addParameter("source", TypeVariableName.invoke("ObjectsSource"))
            .addParameter(
                ParameterSpec
                    .builder("order", TypeVariableName.invoke("ExecutionOrder"))
                    .defaultValue("FixedBottomUpExecOrder()")
                    .build()
            ).addCode("""                
                scheme = GeneratorScheme(listOf($astStr))
                parser = getLangParser(scheme)
                genObjects = GeneratedObjects(
                    source,
                    scheme,
                    listOf(${scheme.objects.joinToString(", ") { "%T::class" }}),
                    listOf(${scheme.filters.joinToString(", ") { "%T::class" }})
                )
            """.trimIndent(),
                *((scheme.objects.asSequence() + scheme.filters.asSequence())
                    .map { ClassName(joinPackages(pack, "objects"), it.name) }
                    .toList().toTypedArray())
            )
            .build()

        res
            .addProperty("scheme", TypeVariableName.invoke("GeneratorScheme"), KModifier.PRIVATE)
            .addProperty("genObjects", TypeVariableName.invoke("GeneratedObjects"), KModifier.PRIVATE)
            .addProperty("parser", ClassName(joinPackages(pack, "parser", "utils"), "Parser").parameterizedBy(TypeVariableName.invoke("FindQuery")))

        res.primaryConstructor(constructor)
            .addProperty(
                PropertySpec
                    .builder("source", TypeVariableName.invoke("ObjectsSource"))
                    .initializer("source")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addProperty(
                PropertySpec
                    .builder("orderGen", TypeVariableName.invoke("ExecutionOrder"))
                    .initializer("order")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

        res.addFunction(
            FunSpec.builder("execute")
                .addParameter("query", String::class)
                .returns(listClass.parameterizedBy(TypeVariableName.invoke("GeneratedObject")))
                .addCode(
                    """
                        val fquery = parser.parse(query.inp()).unwrap()
                        val execGraph = ExecutionGraph(scheme, genObjects, fquery)
                        return execGraph.execute(orderGen)
                    """.trimIndent()
                )
                .build()
        )

        return res.build()
    }

    private fun genSourceInterface(scheme: GeneratorScheme, basePack: String): TypeSpec {
        val res = TypeSpec.interfaceBuilder("ObjectsSource")
        val listClass = ClassName("kotlin.collections", "List")


        scheme.objects.forEach { obj ->
            if (obj.source) {
                val getAll = FunSpec
                    .builder("getAll${obj.name}")
                    .addModifiers(KModifier.ABSTRACT)

                val retObject = ClassName(joinPackages(basePack, "objects"), obj.name)
                val parametrized = listClass.parameterizedBy(retObject)
                getAll.returns(parametrized)

                res.addFunction(getAll.build())
            }

            for (m in obj.members) {
                if (m.isSource) {
                    val getSourceProperty = FunSpec
                        .builder("get${obj.name}By${m.memName.capitalize()}")
                        .addParameter("v", TypeVariableName.invoke("ValueObject"))
                        .addModifiers(KModifier.ABSTRACT)

                    val retObject = ClassName(joinPackages(basePack, "objects"), obj.name)
                    val parameterized = listClass.parameterizedBy(retObject)
                    getSourceProperty.returns(parameterized)

                    res.addFunction(getSourceProperty.build())
                }
            }
        }

        return res.build()
    }
}