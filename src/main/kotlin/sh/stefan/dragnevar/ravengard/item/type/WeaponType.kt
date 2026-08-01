package sh.stefan.dragnevar.ravengard.item.type

import sh.stefan.dragnevar.ravengard.RavengardLabel

enum class WeaponType(override val character: Char) : RavengardLabel {
    BOW('\uE201'),
    CROSSBOW('\uE206'),
    DAGGER('\uE207'),
    GREATAXE('\uE20B'),
    GREATSWORD('\uE20C'),
    HALBERD('\uE20D'),
    HAMMER('\uE20E'),
    MACE('\uE213'),
    SHIELD('\uE219'),
    SWORD('\uE21A'),
    KNIFE('\uE222')
}
