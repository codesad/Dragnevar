package sh.stefan.dragnevar.ravengard.item

import sh.stefan.dragnevar.ravengard.Rarity
import sh.stefan.dragnevar.ravengard.item.type.ConsumableType

class RavengardConsumable private constructor(
    data: RavengardItemData,
    rarity: Rarity,
    val type: ConsumableType,
    val healing: Double,
    val healingDurationSeconds: Double?
) : RavengardItem(data, rarity) {

    override val idealSlot = null
    override val group = RavengardItemGroup.Consumable(type)

    override fun compareWith(other: RavengardItem): Int {
        require(other is RavengardConsumable && other.type == type) {
            "Cannot compare $type consumable with ${other.group}"
        }
        return Comparator.compare(this, other)
    }

    companion object Comparator :
        kotlin.Comparator<RavengardConsumable>,
        RavengardItemParser<RavengardConsumable> {

        override fun from(data: RavengardItemData): RavengardConsumable? {
            val rarity = data.rarity ?: return null
            val type = data.consumableType ?: return null
            val healing = data.healing ?: return null

            return RavengardConsumable(
                data,
                rarity,
                type,
                healing,
                data.healingDurationSeconds
            )
        }

        override fun compare(first: RavengardConsumable, second: RavengardConsumable): Int {
            return compareValuesBy(
                first,
                second,
                RavengardConsumable::healing,
                RavengardConsumable::rarity
            )
        }
    }
}
