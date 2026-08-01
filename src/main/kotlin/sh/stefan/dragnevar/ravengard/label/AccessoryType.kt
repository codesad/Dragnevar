package sh.stefan.dragnevar.ravengard.label

enum class AccessoryType(
    override val character: Char,
    val equippedSlot: EquipmentSlot
) : RavengardLabel {
    NECKLACE('\uE216', EquipmentSlot.NECKLACE),
    EARRING('\uE22B', EquipmentSlot.EARRING),
    BELT('\uE22C', EquipmentSlot.BELT),
    RING('\uE22D', EquipmentSlot.RING)
}
