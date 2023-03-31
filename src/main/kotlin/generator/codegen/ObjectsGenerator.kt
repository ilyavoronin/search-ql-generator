package generator.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import generator.GeneratorScheme
import generator.ast.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList

class ObjectsGenerator {
    fun genCode(path: String, pack: String, scheme: GeneratorScheme) {
        if (!Files.exists(Paths.get(path))) {
            Files.createDirectory(Paths.get(path))
        }
        val modifiers = this.genModifiers(scheme.modifiers)
        val interfaces = this.genInterfaces(scheme.interfaces)
        val objects = this.genObjects(scheme.objects)
        val filters = this.genFilters(scheme.filters)

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

        fileInt.build().writeTo(Paths.get(path))
        fileMods.build().writeTo(Paths.get(path))
        fileFilters.build().writeTo(Paths.get(path))
        fileObjects.build().writeTo(Paths.get(path))
    }

    private fun genModifiers(modifierSpecs: List<Modifier>): Map<String, TypeSpec> {
        val res = mutableMapOf<String, TypeSpec>()
        for (modSpec in modifierSpecs) {
            val mod = TypeSpec.interfaceBuilder(modSpec.name.cap() + "Mod")
            res[modSpec.name] = mod.build()
        }

        return res
    }

    private fun genObjects(filterSpecs: List<Object>): Map<String, TypeSpec> {
        val res = mutableMapOf<String, TypeSpec>()

        for (intSpec in filterSpecs) {
            val int = TypeSpec.interfaceBuilder(intSpec.name)
            intSpec.inheritedFrom?.let {int.addSuperinterface(TypeVariableName.invoke(it)) }
            for (fieldSpec in intSpec.members) {
                val f =  FunSpec
                    .builder("get${fieldSpec.memName.cap()}")
                    .addModifiers(KModifier.ABSTRACT)
                if (fieldSpec.isMany) {
                    val listName = ClassName("kotlin.collections", "List")
                    val parametrized = listName.parameterizedBy(TypeVariableName.invoke(fieldSpec.memType))
                    f.returns(parametrized)
                } else {
                    f.returns(TypeVariableName.invoke(fieldSpec.memType))

                }

                for (modifierSpec in fieldSpec.modifiers) {
                    val param = ParameterSpec.builder(modifierSpec.lowercase(), TypeVariableName.invoke(modifierSpec.cap() + "Mod"))
                    f.addParameter(param.build())
                }
                int.addFunction(f.build())
            }
            res[intSpec.name] = int.build()
        }

        return res;
    }

    private fun genFilters(filterSpecs: List<Filter>): Map<String, TypeSpec> {
        val res = mutableMapOf<String, TypeSpec>()

        for (intSpec in filterSpecs) {
            val int = TypeSpec.interfaceBuilder(intSpec.name)
            intSpec.inheritedFrom?.let {int.addSuperinterface(TypeVariableName.invoke(it)) }
            for (fieldSpec in intSpec.members) {
                val f =  FunSpec
                    .builder("get${fieldSpec.memName.cap()}")
                    .addModifiers(KModifier.ABSTRACT)

                if (fieldSpec.isMany) {
                    val listName = ClassName("kotlin.collections", "List")
                    val parametrized = listName.parameterizedBy(TypeVariableName.invoke(fieldSpec.memType))
                    f.returns(parametrized)
                } else {
                    f.returns(TypeVariableName.invoke(fieldSpec.memType))
                }

                for (modifierSpec in fieldSpec.modifiers) {
                    val param = ParameterSpec.builder(modifierSpec.lowercase(), TypeVariableName.invoke(modifierSpec.cap() + "Mod"))
                    f.addParameter(param.build())
                }
                int.addFunction(f.build())
            }
            res[intSpec.name] = int.build()
        }

        return res;
    }

    private fun genInterfaces(interfaceSpecs: List<Interface>): Map<String, TypeSpec> {
        val res = mutableMapOf<String, TypeSpec>()

        for (intSpec in interfaceSpecs) {
            val int = TypeSpec.interfaceBuilder(intSpec.name)
            intSpec.inheritedFrom?.let {int.addSuperinterface(TypeVariableName.invoke(it)) }
            for (fieldSpec in intSpec.members) {
                val f =  FunSpec
                    .builder("get${fieldSpec.memName.cap()}")
                    .addModifiers(KModifier.ABSTRACT)

                if (fieldSpec.isMany) {
                    val listName = ClassName("kotlin.collections", "List")
                    val parametrized = listName.parameterizedBy(TypeVariableName.invoke(fieldSpec.memType))
                    f.returns(parametrized)
                } else {
                    f.returns(TypeVariableName.invoke(fieldSpec.memType))
                }

                for (modifierSpec in fieldSpec.modifiers) {
                    val param = ParameterSpec.builder(modifierSpec.lowercase(), TypeVariableName.invoke(modifierSpec.cap() + "Mod"))
                    f.addParameter(param.build())
                }
                int.addFunction(f.build())
            }
            res[intSpec.name] = int.build()
        }

        return res
    }

    private fun String.cap() = this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.getDefault()
        ) else it.toString()
    }
}