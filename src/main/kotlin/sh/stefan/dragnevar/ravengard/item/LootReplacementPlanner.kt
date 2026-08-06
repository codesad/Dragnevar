package sh.stefan.dragnevar.ravengard.item

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot

internal object LootReplacementPlanner {
    private val equipmentSlotIndices = EquipmentSlot.entries
        .map(EquipmentSlot::inventoryIndex)
        .toSet()
    private val positionOrder = compareBy<PricedSlot> { it.x }
        .thenBy { it.y }

    fun plan(
        menu: AbstractContainerMenu,
        playerInventory: Inventory
    ): List<Replacement> {
        val activeSlots = menu.slots.filter { it.isActive }
        val playerSlots = activeSlots.filter { slot ->
            slot.container === playerInventory &&
                slot.containerSlot !in equipmentSlotIndices
        }
        val lootSlots = activeSlots.filter { it.container !== playerInventory }
        if (lootSlots.isEmpty() || playerSlots.isEmpty() || playerSlots.any { !it.hasItem() }) {
            return emptyList()
        }

        val lootCandidates = lootSlots.pricedSlots().sortedWith(
            compareByDescending<PricedSlot> { it.value }
                .thenBy { it.menuSlotIndex }
        )
        val inventoryCandidates = playerSlots.pricedSlots().sortedWith(
            compareBy<PricedSlot> { it.value }
                .thenBy { it.menuSlotIndex }
        )

        val profitablePairs = lootCandidates.zip(inventoryCandidates)
            .takeWhile { (loot, inventory) -> loot.value > inventory.value }

        val selectedLoot = profitablePairs
            .map { it.first }
            .sortedWith(positionOrder)
        val selectedInventory = profitablePairs
            .map { it.second }
            .sortedWith(positionOrder)

        return selectedLoot.zip(selectedInventory)
            .map { (loot, inventory) ->
                Replacement(
                    loot.menuSlotIndex,
                    inventory.menuSlotIndex
                )
            }
    }

    private fun Iterable<Slot>.pricedSlots(): List<PricedSlot> = mapNotNull { slot ->
        if (!slot.hasItem()) return@mapNotNull null
        val data = RavengardItemData(slot.item)
        if (data.consumableType != null) return@mapNotNull null
        val price = data.price ?: return@mapNotNull null
        PricedSlot(slot.index, slot.x, slot.y, price)
    }

    data class Replacement(
        val lootSlotIndex: Int,
        val inventorySlotIndex: Int
    )

    private data class PricedSlot(
        val menuSlotIndex: Int,
        val x: Int,
        val y: Int,
        val value: Int
    )
}
