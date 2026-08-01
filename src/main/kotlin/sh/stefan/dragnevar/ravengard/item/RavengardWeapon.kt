package sh.stefan.dragnevar.ravengard.item

import net.minecraft.world.item.ItemStack
import sh.stefan.dragnevar.ravengard.Rarity
import sh.stefan.dragnevar.ravengard.item.type.WeaponType

class RavengardWeapon private constructor(
    stack: ItemStack,
    rarity: Rarity,
    val type: WeaponType,
    val damage: Double,
    val attackSpeed: Double
) : RavengardItem(stack, rarity) {

    override val idealSlot = null
    override val group = RavengardItemGroup.Weapon

    override fun compareWith(other: RavengardItem): Int {
        require(other is RavengardWeapon) { "Cannot compare a weapon with ${other.group}" }
        return compare(this, other)
    }

    companion object Comparator :
        kotlin.Comparator<RavengardWeapon>,
        RavengardItemParser<RavengardWeapon> {

        override fun from(data: RavengardItemData): RavengardWeapon? {
            val rarity = data.rarity ?: return null
            val type = data.weaponType ?: return null
            val damage = data.damage ?: return null
            val attackSpeed = data.attackSpeed ?: return null

            return RavengardWeapon(data.stack, rarity, type, damage, attackSpeed)
        }

        override fun compare(first: RavengardWeapon, second: RavengardWeapon): Int {
            return compareValuesBy(
                first,
                second,
                RavengardWeapon::damage,
                RavengardWeapon::attackSpeed,
                RavengardWeapon::rarity
            )
        }
    }
}
