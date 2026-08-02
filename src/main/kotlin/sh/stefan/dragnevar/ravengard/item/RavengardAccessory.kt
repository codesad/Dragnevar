package sh.stefan.dragnevar.ravengard.item

import sh.stefan.dragnevar.ravengard.Rarity
import sh.stefan.dragnevar.ravengard.item.type.AccessoryType

class RavengardAccessory private constructor(
    data: RavengardItemData,
    rarity: Rarity,
    val type: AccessoryType
) : RavengardItem(data, rarity) {

    override val idealSlot = type.equippedSlot
    override val group = RavengardItemGroup.Accessory(type)

    override fun compareWith(other: RavengardItem): Int {
        require(other is RavengardAccessory && other.type == type) {
            "Cannot compare $type accessory with ${other.group}"
        }
        return Comparator.compare(this, other)
    }

    companion object Comparator :
        kotlin.Comparator<RavengardAccessory>,
        RavengardItemParser<RavengardAccessory> {

        override fun from(data: RavengardItemData): RavengardAccessory? {
            val rarity = data.rarity ?: return null
            val type = data.accessoryType ?: return null

            return RavengardAccessory(data, rarity, type)
        }

        override fun compare(first: RavengardAccessory, second: RavengardAccessory): Int {
            return first.rarity.compareTo(second.rarity)
        }
    }
}
