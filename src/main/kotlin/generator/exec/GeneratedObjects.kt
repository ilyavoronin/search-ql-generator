package generator.exec

import generator.GeneratedObject
import generator.scheme.ExtendedDefField
import generator.scheme.GeneratorScheme
import generator.scheme.ast.ModValueType
import java.lang.IllegalStateException
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

interface ObjectsSource {}

class GeneratedObjects(
    private val source: ObjectsSource,
    private val scheme: GeneratorScheme,
    objs: List<KClass<out GeneratedObject>>,
    filters: List<KClass<out GeneratedObject>>,
    val testVersion: Boolean
) {
    class GetObjectMethod(
        private val scheme: GeneratorScheme,
        private val get: KCallable<*>
    ) {
        fun call(obj: GeneratedObject, field: ExtendedDefField, modifiers: Map<String, ModValueType>): Any? {
            val mods = field.modifiers.map {
                if (modifiers.containsKey(it)) {
                    modifiers[it]!!.getValue()
                } else {
                    scheme.getModifier(it)!!.type.getValue()
                }
            }.toTypedArray()
            return get.call(obj, *mods)
        }

        fun callToList(obj: GeneratedObject, field: ExtendedDefField, modifiers: Map<String, ModValueType>): List<GeneratedObject> {
            val res = call(obj, field, modifiers)

            return if (field.isMany) {
                res as List<GeneratedObject>
            } else {
                listOf(res as GeneratedObject)
            }
        }
    }

    class GetRevMethod(
        private val scheme: GeneratorScheme,
        private val get: KCallable<*>,
    ) {
        fun call(obj: GeneratedObject, field: ExtendedDefField, modifiers: Map<String, ModValueType>): List<GeneratedObject> {
            val mods = field.modifiers.mapNotNull {
                val mod = scheme.getModifier(it)!!
                if (mod.revAllowed) {
                    if (modifiers.containsKey(it)) {
                        modifiers[it]!!.getValue()
                    } else {
                        scheme.getModifier(it)!!.type.getValue()
                    }
                } else {
                    if (modifiers.containsKey(mod.name)) {
                        throw IllegalStateException()
                    }
                    null
                }
            }.toTypedArray()
            return get.call(obj, *mods) as List<GeneratedObject>
        }
    }

    private val defsClasses = mutableMapOf<String, KClass<*>>()
    init {
        objs.forEach { defsClasses[it.simpleName.toString()] = it }
        filters.forEach { defsClasses[it.simpleName.toString()] = it }
    }

    fun getDef(name: String): KClass<*> {
        return defsClasses[name]!!
    }

    fun getDefMethod(name: String, subObjName: String): GetObjectMethod {
        val methodName = "get${subObjName.capitalize()}"
        return GetObjectMethod(scheme, defsClasses[name]!!.members.single { it.name == methodName })
    }

    fun getRevDefMethod(name: String, revName: String): GetRevMethod {
        if (testVersion) {
            return GetRevMethod(scheme, this::getAllObjectsByProperty)
        }
        val methodName = "parent${revName.capitalize()}"
        return GetRevMethod(scheme, defsClasses[name]!!.members.single { it.name == methodName })
    }

    fun getAllObjectsOfType(t: String): List<GeneratedObject> {
        val methodName = "getAll$t"
        val method = ObjectsSource::class.members.single { it.name == methodName }

        return method.call(source) as List<GeneratedObject>
    }

    fun getAllObjectsByProperty(obj: String, property: String, value: ValueObject): List<GeneratedObject> {
        val methodName = "get${obj.capitalize()}By${property.capitalize()}"
        val method = ObjectsSource::class.members.single { it.name == methodName }

        return method.call(source, value) as List<GeneratedObject>

    }
}