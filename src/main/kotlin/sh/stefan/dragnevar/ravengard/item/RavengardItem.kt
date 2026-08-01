package sh.stefan.dragnevar.ravengard.item

import net.minecraft.world.item.ItemStack
import sh.stefan.dragnevar.ravengard.Rarity
import sh.stefan.dragnevar.ravengard.item.type.AccessoryType
import sh.stefan.dragnevar.ravengard.item.type.ArmorType
import sh.stefan.dragnevar.ravengard.item.type.ConsumableType

sealed interface RavengardItemGroup {
    data class Armor(val type: ArmorType) : RavengardItemGroup
    data class Accessory(val type: AccessoryType) : RavengardItemGroup
    data class Consumable(val type: ConsumableType) : RavengardItemGroup

    // weapon types share one group since we only want the best weapon overall
    data object Weapon : RavengardItemGroup
}

sealed class RavengardItem(
    val stack: ItemStack,
    val rarity: Rarity
) {
    abstract val idealSlot: EquipmentSlot?
    abstract val group: RavengardItemGroup

    internal abstract fun compareWith(other: RavengardItem): Int
}
