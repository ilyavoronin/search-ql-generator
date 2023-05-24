
object Id: int

source object Obj1 {
    id: Id
	obj2: Obj2 [rev, many]
	obj3: Obj3 [rev, many]
	obj4: Obj4 [rev, many]
}

source object Obj2 {
    id: Id
    obj4: Obj4 [rev, many]
    obj5: Obj5 [rev, many]
    obj6: Obj6 [rev, many]
    obj9: Obj9 [rev, many]
}

object Obj9 {
    id: Id
    obj10: Obj10 [rev, many]
}

object Obj10 {
    id: Id
    obj11: Obj11 [rev, many]
    obj12: Obj12 [rev, many]
}

object Obj11 {
    id: Id [source]
}

object Obj12 {
    id: Id
}

object Obj3 {
    obj7: Obj7 [rev, many]
    obj8: Obj8 [rev, many]
}

object Obj4 {
    id: Id
}

object Obj5 {
    id: Id [source]
}

object Obj6 {
    id: Id
}

object Obj7 {
    id: Id
}

object Obj8 {
    id: Id
}