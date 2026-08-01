package sh.stefan.dragnevar.ravengard.item

import net.minecraft.world.item.ItemStack
import sh.stefan.dragnevar.ravengard.Rarity
import sh.stefan.dragnevar.ravengard.item.type.ArmorType

class RavengardArmor private constructor(
    stack: ItemStack,
    rarity: Rarity,
    val type: ArmorType,
    val defense: Double
) : RavengardItem(stack, rarity) {

    override val idealSlot = type.equippedSlot
    override val group = RavengardItemGroup.Armor(type)

    override fun compareWith(other: RavengardItem): Int {
        require(other is RavengardArmor && other.type == type) {
            "Cannot compare $type armor with ${other.group}"
        }
        return Comparator.compare(this, other)
    }

    companion object Comparator :
        kotlin.Comparator<RavengardArmor>,
        RavengardItemParser<RavengardArmor> {

        override fun from(data: RavengardItemData): RavengardArmor? {
            val rarity = data.rarity ?: return null
            val type = data.armorType ?: return null
            val defense = data.defense ?: return null

            return RavengardArmor(data.stack, rarity, type, defense)
        }

        override fun compare(first: RavengardArmor, second: RavengardArmor): Int {
            return compareValuesBy(
                first,
                second,
                RavengardArmor::defense,
                RavengardArmor::rarity
            )
        }
    }
}
