package sh.stefan.dragnevar.ravengard.item.type

import sh.stefan.dragnevar.ravengard.item.EquipmentSlot
import sh.stefan.dragnevar.ravengard.RavengardLabel

enum class ArmorType(
    override val character: Char,
    val equippedSlot: EquipmentSlot
) : RavengardLabel {
    HELMET('\uE20F', EquipmentSlot.HELMET),
    CHESTPLATE('\uE202', EquipmentSlot.CHESTPLATE),
    LEGGINGS('\uE212', EquipmentSlot.LEGGINGS),
    BOOTS('\uE209', EquipmentSlot.BOOTS)
}
