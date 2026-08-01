package sh.stefan.dragnevar.ravengard.item

// these match slot.containerSlot, not slot.index; e.g. boots stay 36 even if their menu index shifts
enum class EquipmentSlot(val inventoryIndex: Int) {
    HELMET(39),
    CHESTPLATE(38),
    LEGGINGS(37),
    BOOTS(36),
    NECKLACE(9),
    EARRING(10),
    BELT(11),
    RING(12)
}
