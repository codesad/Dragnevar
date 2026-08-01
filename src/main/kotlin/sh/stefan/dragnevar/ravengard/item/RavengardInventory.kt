package sh.stefan.dragnevar.ravengard.item

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import sh.stefan.dragnevar.ravengard.Profile

class RavengardInventory private constructor(
    private val entries: List<Entry>
) {
    fun bestItems(): List<Entry> {
        return entries
            .groupBy { it.item.group }
            .values
            .map { it.maxWith(::compareEntries) }
    }

    private fun compareEntries(first: Entry, second: Entry): Int {
        return first.item.compareWith(second.item)
            .takeIf { it != 0 }
            // if they're equal, keep the one that's already in its proper slot
            ?: first.isInIdealSlot.compareTo(second.isInIdealSlot)
    }

    class Entry(
        val menuSlotIndex: Int,
        val item: RavengardItem,
        internal val isInIdealSlot: Boolean
    )

    companion object {
        // start at the bottom, but still go left to right inside each row
        private val PLAYER_SLOT_ORDER =
            compareByDescending<Slot> { it.y }
                .thenBy { it.x }

        fun from(
            menu: AbstractContainerMenu,
            profile: Profile,
            playerInventory: Inventory
        ): RavengardInventory {
            val entries = menu.slotsInPriorityOrder(playerInventory).mapNotNull { slot ->
                val stack = slot.item
                if (stack.isEmpty) return@mapNotNull null

                val data = RavengardItemData(stack)
                if (!data.isCompatibleWith(profile)) {
                    return@mapNotNull null
                }

                RavengardItemFactory.create(data)?.let { item ->
                    Entry(
                        menuSlotIndex = slot.index,
                        item = item,
                        isInIdealSlot = slot.isIdealSlotFor(item, playerInventory)
                    )
                }
            }
            return RavengardInventory(entries)
        }

        private fun AbstractContainerMenu.slotsInPriorityOrder(
            playerInventory: Inventory
        ): List<Slot> {
            val (playerSlots, containerSlots) = slots.partition { slot ->
                slot.belongsTo(playerInventory)
            }

            // player items should win ties, then we scan the opened menu backwards
            return playerSlots.sortedWith(PLAYER_SLOT_ORDER) + containerSlots.reversed()
        }

        private fun Slot.isIdealSlotFor(
            item: RavengardItem,
            playerInventory: Inventory
        ): Boolean {
            if (!belongsTo(playerInventory)) return false
            val idealIndex = item.idealSlot?.inventoryIndex ?: return false

            // containerSlot stays consistent even when the menu's slot indexes shift
            return containerSlot == idealIndex
        }

        private fun Slot.belongsTo(playerInventory: Inventory): Boolean {
            return container === playerInventory
        }
    }
}
