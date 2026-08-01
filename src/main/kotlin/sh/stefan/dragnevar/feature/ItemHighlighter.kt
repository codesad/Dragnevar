package sh.stefan.dragnevar.feature

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import sh.stefan.dragnevar.ravengard.Profile
import sh.stefan.dragnevar.ravengard.item.RavengardArmor
import sh.stefan.dragnevar.ravengard.item.RavengardInventory
import sh.stefan.dragnevar.ravengard.item.RavengardItem
import sh.stefan.dragnevar.ravengard.item.RavengardItemData
import sh.stefan.dragnevar.ravengard.item.RavengardItemGroup
import sh.stefan.dragnevar.ravengard.item.type.ArmorType

object ItemHighlighter : Feature(), ContainerOpenFeature, TickFeature {
    private const val ARMOR_COLOR = 0x80007BFF.toInt()
    private const val ACCESSORY_COLOR = 0x8000E65C.toInt()
    private const val WEAPON_COLOR = 0x80B000FF.toInt()
    private const val CONSUMABLE_COLOR = 0x80F54927.toInt()

    // only one menu can be open, so there's only one state to cache
    private var menuState: MenuState? = null

    override fun onContainerOpen(screen: AbstractContainerScreen<*>) {
        refresh(screen.menu)
    }

    override fun onTick() {
        val menu = player?.containerMenu ?: return
        refresh(menu)
    }

    private fun refresh(menu: AbstractContainerMenu) {
        val profile = ClassDetector.currentProfile
        if (profile == null) {
            menuState = null
            return
        }

        val currentPlayer = player
        if (currentPlayer == null) {
            menuState = null
            return
        }

        val playerInventory = currentPlayer.inventory
        // regular container menus don't include the four equipped armor slots
        val hasHiddenArmor = currentPlayer.inventoryMenu !== menu

        val snapshot = snapshotOf(menu, hasHiddenArmor, playerInventory)
        val previousState = menuState
        if (previousState != null &&
            previousState.menu === menu &&
            previousState.snapshot == snapshot
        ) {
            return
        }

        val inventory = RavengardInventory.from(menu, profile, playerInventory)
        val equippedArmor = if (hasHiddenArmor) {
            parseEquippedArmor(playerInventory, profile)
        } else {
            emptyMap()
        }
        val highlightedSlots = findHighlightedSlots(inventory, equippedArmor)

        menuState = MenuState(menu, snapshot, highlightedSlots)
    }

    private fun snapshotOf(
        menu: AbstractContainerMenu,
        includeEquippedArmor: Boolean,
        playerInventory: Inventory
    ): List<Int> {
        // hashing every tick is cheaper than parsing all the item lore every tick
        return buildList {
            menu.slots.mapTo(this) { ItemStack.hashItemAndComponents(it.item) }

            if (includeEquippedArmor) {
                ArmorType.entries.mapTo(this) { armorType ->
                    ItemStack.hashItemAndComponents(
                        playerInventory.getItem(armorType.equippedSlot.inventoryIndex)
                    )
                }
            }
        }
    }

    @JvmStatic
    fun renderSlot(
        menu: AbstractContainerMenu,
        graphics: GuiGraphicsExtractor,
        slot: Slot
    ) {
        val state = menuState?.takeIf { it.menu === menu } ?: return
        val color = state.highlightedSlots[slot.index] ?: return

        graphics.fill(
            slot.x,
            slot.y,
            slot.x + 16,
            slot.y + 16,
            color
        )
    }

    private fun findHighlightedSlots(
        inventory: RavengardInventory,
        equippedArmor: Map<ArmorType, RavengardArmor>
    ): Map<Int, Int> {
        return (inventory.bestItems() + inventory.consumables())
            .filter { isBetterThanEquippedArmor(it.item, equippedArmor) }
            .associate { it.menuSlotIndex to colorOf(it.item) }
    }

    private fun isBetterThanEquippedArmor(
        item: RavengardItem,
        equippedArmor: Map<ArmorType, RavengardArmor>
    ): Boolean {
        val candidate = item as? RavengardArmor ?: return true
        val equipped = equippedArmor[candidate.type] ?: return true
        return RavengardArmor.compare(candidate, equipped) > 0
    }

    private fun parseEquippedArmor(
        inventory: Inventory,
        profile: Profile
    ): Map<ArmorType, RavengardArmor> {
        return ArmorType.entries.mapNotNull { armorType ->
            val stack = inventory.getItem(armorType.equippedSlot.inventoryIndex)
            if (stack.isEmpty) return@mapNotNull null

            val data = RavengardItemData(stack)
            if (!data.isCompatibleWith(profile)) return@mapNotNull null

            RavengardArmor.from(data)?.let { it.type to it }
        }.toMap()
    }

    private fun colorOf(item: RavengardItem): Int {
        return when (item.group) {
            is RavengardItemGroup.Armor -> ARMOR_COLOR
            is RavengardItemGroup.Accessory -> ACCESSORY_COLOR
            is RavengardItemGroup.Consumable -> CONSUMABLE_COLOR
            RavengardItemGroup.Weapon -> WEAPON_COLOR
        }
    }

    private class MenuState(
        val menu: AbstractContainerMenu,
        val snapshot: List<Int>,
        val highlightedSlots: Map<Int, Int>
    )
}
