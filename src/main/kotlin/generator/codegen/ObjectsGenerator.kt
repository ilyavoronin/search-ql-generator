package generator.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import generator.GeneratorScheme
import generator.ast.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class ObjectsGenerator {
    fun genCode(path: String, pack: String, scheme: GeneratorScheme) {
        if (!Files.exists(Paths.get(path))) {
            Files.createDirectory(Paths.get(path))
        }

        val builtIns = this.genBuiltIns()
        val modifiers = this.genModifiers(scheme.modifiers)
        val interfaces = this.genObjects(scheme.interfaces, builtIns)
        val objects = this.genObjects(scheme.objects, builtIns)
        val filters = this.genObjects(scheme.filters, builtIns)

        val fileBuiltIns = FileSpec.builder(pack, "builtins")
        for (b in builtIns.values) {
            fileBuiltIns.addType(b)
        }
        val fileInt = FileSpec.builder(pack, "interfaces")
        for (int in interfaces.values) {
            fileInt.addType(int)
        }
        val fileMods = FileSpec.builder(pack, "modifiers")
        for (mod in modifiers.values) {
            fileMods.addType(mod)
        }
        val fileFilters = FileSpec.builder(pack, "filters")
        for (filt in filters.values) {
            fileFilters.addType(filt)
        }
        val fileObjects = FileSpec.builder(pack, "objects")
        for (obj in objects.values) {
            fileObjects.addType(obj)
        }

        fileBuiltIns.build().writeTo(Paths.get(path))
        fileInt.build().writeTo(Paths.get(path))
        fileMods.build().writeTo(Paths.get(path))
        fileFilters.build().writeTo(Paths.get(path))
        fileObjects.build().writeTo(Paths.get(path))
    }

    private fun genBuiltIns(): Map<String, TypeSpec> {
        val res = mutableMapOf<String, TypeSpec>()
        run {
            val b = TypeSpec.interfaceBuilder("BoolBuiltIn")
            val f = FunSpec.builder("getBool")
                .addModifiers(KModifier.ABSTRACT)
                .returns(Boolean::class.java)
            b.addFunction(f.build())
            res["bool"] = b.build()
        }
        run {
            val b = TypeSpec.interfaceBuilder("StringBuiltIn")
            val f = FunSpec.builder("getString")
                .addModifiers(KModifier.ABSTRACT)
                .returns(String::class.java)
            b.addFunction(f.build())
            res["string"] = b.build()
        }
        run {
            val b = TypeSpec.interfaceBuilder("IntBuiltIn")
            val f = FunSpec.builder("getInt")
                .addModifiers(KModifier.ABSTRACT)
                .returns(Int::class.java)
            b.addFunction(f.build())
            res["int"] = b.build()
        }
        return res
    }

    private fun genModifiers(modifierSpecs: List<Modifier>): Map<String, TypeSpec> {
        val res = mutableMapOf<String, TypeSpec>()
        for (modSpec in modifierSpecs) {
            val mod = TypeSpec.interfaceBuilder(modSpec.name.cap() + "Mod")
            res[modSpec.name] = mod.build()
        }

        return res
    }

    private fun genObjects(filterSpecs: List<Definition>, builtInSPecs: Map<String, TypeSpec>): Map<String, TypeSpec> {
        val res = mutableMapOf<String, TypeSpec>()
        val paramBuiltIns = mapOf(
            Pair("bool", "Boolean"),
            Pair("string", "String"),
            Pair("int", "Int")
        )

        for (objSpec in filterSpecs) {
            val int = TypeSpec.interfaceBuilder(objSpec.name)
            objSpec.inheritedFrom?.let {
                int.addSuperinterface(TypeVariableName.invoke(builtInSPecs[it]?.name ?: it))
            }
            for (fieldSpec in objSpec.members) {
                val f =  FunSpec
                    .builder("get${fieldSpec.memName.cap()}")
                    .addModifiers(KModifier.ABSTRACT)

                val type = TypeVariableName.invoke(paramBuiltIns[fieldSpec.memType] ?: fieldSpec.memType)
                if (fieldSpec.isMany) {
                    val listName = ClassName("kotlin.collections", "List")
                    val parametrized = listName.parameterizedBy(type)
                    f.returns(parametrized)
                } else {
                    f.returns(type)
                }

                for (modifierSpec in fieldSpec.modifiers) {
                    val param = ParameterSpec.builder(modifierSpec.lowercase(), TypeVariableName.invoke(modifierSpec.cap() + "Mod"))
                    f.addParameter(param.build())
                }
                int.addFunction(f.build())
            }
            res[objSpec.name] = int.build()
        }

        return res;
    }

    private fun String.cap() = this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.getDefault()
        ) else it.toString()
    }
}