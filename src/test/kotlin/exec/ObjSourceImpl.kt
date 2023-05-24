package exec

import generated.exec.exec.ObjectsSource
import generated.exec.exec.ValueObject
import generated.exec.objects.Obj1
import generated.exec.objects.Obj11
import generated.exec.objects.Obj2
import generated.exec.objects.Obj5

class ObjSourceImpl : ObjectsSource {
    override fun getAllObj1(): List<Obj1> {
        return objs.getAllObjs(O1::class)
    }

    override fun getAllObj2(): List<Obj2> {
        return objs.getAllObjs(O2::class)
    }

    override fun getObj11ById(v: ValueObject): List<Obj11> {
        val id = (v as ValueObject.Int).v
        return objs.idTOObj[id]?.let { obj5 ->
            if (obj5 is Obj11) {
                listOf(obj5 as Obj11)
            } else {
                listOf()
            }
        } ?: listOf()
    }

    override fun getObj5ById(v: ValueObject): List<Obj5> {
        val id = (v as ValueObject.Int).v
        return objs.idTOObj[id]?.let { obj5 ->
            if (obj5 is Obj5) {
                listOf(obj5 as Obj5)
            } else {
                listOf()
            }
        } ?: listOf()
    }
}