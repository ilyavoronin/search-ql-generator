package generator.exec

import generator.GeneratedObject
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class GeneratedObjects(
    objs: List<Pair<KClass<*>, String>>,
    filters: List<Pair<KClass<*>, String>>
) {
    private val defsClasses = mutableMapOf<String, KClass<*>>()
    init {
        objs.forEach { defsClasses[it.second] = it.first }
        filters.forEach { defsClasses[it.second] = it.first }
    }

    fun getDef(name: String): KClass<*> {
        return defsClasses[name]!!
    }

    fun getDefMethod(name: String, subObjName: String): KCallable<*> {
        return defsClasses[name]!!.members.single { it.name == subObjName }
    }

    fun getRevDefMethod(name: String, revName: String): KCallable<*> {
        return defsClasses[name]!!.members.single { it.name == revName }
    }

    fun getAllObjectsOfType(t: String): List<GeneratedObject> {
        return listOf()
    }

    fun getAllObjectsByProperty(obj: String, property: String, value: ValueObject): List<GeneratedObject> {
        return listOf()
    }
}