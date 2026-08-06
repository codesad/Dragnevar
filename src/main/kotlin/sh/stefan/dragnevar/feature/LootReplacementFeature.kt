package sh.stefan.dragnevar.feature

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import sh.stefan.dragnevar.config.DragnevarConfig
import sh.stefan.dragnevar.ravengard.item.LootReplacementPlanner
import sh.stefan.dragnevar.utils.Render

object LootReplacementFeature : Feature(), TickFeature {
    private const val SLOT_CENTER = 8
    private const val NORMAL_ALPHA = 0xD0
    private const val DIMMED_ALPHA = 0x48
    private const val HOVERED_ALPHA = 0xFF
    private const val GOLD_RGB = 0xF4C95D
    private const val HOVERED_GOLD_RGB = 0xFFE7A3

    private var menuState: MenuState? = null

    override fun onTick() {
        val menu = player?.containerMenu ?: return
        refresh(menu)
    }

    private fun refresh(menu: AbstractContainerMenu) {
        if (!DragnevarConfig.values.items.showLootReplacementLines) {
            menuState = null
            return
        }
        if (!menu.carried.isEmpty) return

        val currentPlayer = player
        if (currentPlayer == null) {
            menuState = null
            return
        }

        val snapshot = menu.slots.associate { slot ->
            slot.index to ItemStack.hashItemAndComponents(slot.item)
        }
        val previousState = menuState
        if (previousState?.menu === menu && previousState.snapshot == snapshot) return

        menuState = MenuState(
            menu,
            snapshot,
            LootReplacementPlanner.plan(menu, currentPlayer.inventory)
        )
    }

    @JvmStatic
    fun renderContainerBackground(
        menu: AbstractContainerMenu,
        graphics: GuiGraphicsExtractor,
        hoveredSlot: Slot?,
        mouseX: Int,
        mouseY: Int
    ) {
        val state = menuState?.takeIf { it.menu === menu } ?: return
        val replacements = state.replacements
        if (replacements.isEmpty()) return

        val slotsByIndex = menu.slots.associateBy(Slot::index)
        val carriedHash = menu.carried
            .takeUnless(ItemStack::isEmpty)
            ?.let(ItemStack::hashItemAndComponents)
        val hoveredPair = replacements.indexOfFirst { replacement ->
            hoveredSlot?.index == replacement.lootSlotIndex ||
                hoveredSlot?.index == replacement.inventorySlotIndex
        }

        replacements.forEachIndexed { index, replacement ->
            val lootSlot = slotsByIndex[replacement.lootSlotIndex] ?: return@forEachIndexed
            val inventorySlot = slotsByIndex[replacement.inventorySlotIndex]
                ?: return@forEachIndexed
            val originalLootHash = state.snapshot[replacement.lootSlotIndex]
                ?: return@forEachIndexed
            val originalInventoryHash = state.snapshot[replacement.inventorySlotIndex]
                ?: return@forEachIndexed
            val currentLootHash = itemHashOf(lootSlot)
            val currentInventoryHash = itemHashOf(inventorySlot)
            val lootIsCarried = currentLootHash == null && carriedHash == originalLootHash

            if (currentInventoryHash == originalLootHash) return@forEachIndexed
            if (currentLootHash != originalLootHash && !lootIsCarried) return@forEachIndexed
            if (currentInventoryHash != null && currentInventoryHash != originalInventoryHash) {
                return@forEachIndexed
            }

            val emphasized = index == hoveredPair || lootIsCarried
            val alpha = when {
                emphasized -> HOVERED_ALPHA
                hoveredPair >= 0 && carriedHash == null -> DIMMED_ALPHA
                else -> NORMAL_ALPHA
            }
            val color = (alpha shl 24) or if (emphasized) {
                HOVERED_GOLD_RGB
            } else {
                GOLD_RGB
            }
            val lootPosition = if (lootIsCarried) {
                SlotPosition(mouseX, mouseY)
            } else {
                centerOf(lootSlot)
            }
            val inventoryPosition = centerOf(inventorySlot)

            Render.drawDashedLine(
                graphics,
                lootPosition.x,
                lootPosition.y,
                inventoryPosition.x,
                inventoryPosition.y,
                color
            )
        }
    }

    private fun itemHashOf(slot: Slot): Int? = slot.item
        .takeUnless(ItemStack::isEmpty)
        ?.let(ItemStack::hashItemAndComponents)

    private fun centerOf(slot: Slot) = SlotPosition(
        slot.x + SLOT_CENTER,
        slot.y + SLOT_CENTER
    )

    private class MenuState(
        val menu: AbstractContainerMenu,
        val snapshot: Map<Int, Int>,
        val replacements: List<LootReplacementPlanner.Replacement>
    )

    private data class SlotPosition(val x: Int, val y: Int)
}
