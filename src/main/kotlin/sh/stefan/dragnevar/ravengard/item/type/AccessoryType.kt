package sh.stefan.dragnevar.ravengard.item.type

import sh.stefan.dragnevar.ravengard.item.EquipmentSlot
import sh.stefan.dragnevar.ravengard.RavengardLabel

enum class AccessoryType(
    override val character: Char,
    val equippedSlot: EquipmentSlot
) : RavengardLabel {
    NECKLACE('\uE216', EquipmentSlot.NECKLACE),
    EARRING('\uE22B', EquipmentSlot.EARRING),
    BELT('\uE22C', EquipmentSlot.BELT),
    RING('\uE22D', EquipmentSlot.RING)
}
