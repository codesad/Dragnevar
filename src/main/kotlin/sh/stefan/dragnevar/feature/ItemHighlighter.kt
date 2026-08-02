package sh.stefan.dragnevar.feature

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import sh.stefan.dragnevar.ravengard.Profile
import sh.stefan.dragnevar.ravengard.item.RavengardArmor
import sh.stefan.dragnevar.ravengard.item.RavengardInventory
import sh.stefan.dragnevar.ravengard.item.RavengardItem
import sh.stefan.dragnevar.ravengard.item.RavengardItemData
import sh.stefan.dragnevar.ravengard.item.RavengardItemGroup
import sh.stefan.dragnevar.ravengard.item.type.ArmorType
import sh.stefan.dragnevar.utils.ItemRender

object ItemHighlighter : Feature(), ContainerOpenFeature, TickFeature {
    private const val ARMOR_COLOR = 0xA0007BFF.toInt()
    private const val ACCESSORY_COLOR = 0xA000E65C.toInt()
    private const val WEAPON_COLOR = 0xA0B000FF.toInt()
    private const val CONSUMABLE_COLOR = 0xA0F54927.toInt()
    private const val PRICE_COLOR = 0xFFFFD700.toInt()
    private const val HEALING_COLOR = 0xFFFF5555.toInt()
    private const val HEALING_DURATION_COLOR = 0xFFFFFF55.toInt()
    private const val TEXT_SCALE = 0.5f
    private const val PROFILE_TEXT_Y_OFFSET = 11
    private const val TEXT_BACKGROUND_COLOR = 0x60000000

    private val PROFILE_COLORS = mapOf(
        Profile.WARRIOR to 0xFFFF5555.toInt(),
        Profile.HUNTER to 0xFF55FF55.toInt(),
        Profile.KNIGHT to 0xFF55AAFF.toInt(),
        Profile.ASSASSIN to 0xFFCC66FF.toInt()
    )

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
        if (!menu.carried.isEmpty) return

        val profile = ClassDetector.currentProfile
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
            previousState.profile == profile &&
            previousState.snapshot == snapshot
        ) {
            return
        }

        val itemOverlays = findItemOverlays(menu)
        val highlightedSlots = if (profile != null) {
            val inventory = RavengardInventory.from(menu, profile, playerInventory)
            val equippedArmor = if (hasHiddenArmor) {
                parseEquippedArmor(playerInventory, profile)
            } else {
                emptyMap()
            }
            findHighlightedSlots(inventory, equippedArmor)
        } else {
            emptyMap()
        }

        menuState = MenuState(menu, profile, snapshot, highlightedSlots, itemOverlays)
    }

    private fun snapshotOf(
        menu: AbstractContainerMenu,
        includeEquippedArmor: Boolean,
        playerInventory: Inventory
    ): MenuSnapshot {
        // hashing every tick is cheaper than parsing all the item lore every tick
        return MenuSnapshot(
            slotHashes = menu.slots.associate { slot ->
                slot.index to ItemStack.hashItemAndComponents(slot.item)
            },
            equippedArmorHashes = if (includeEquippedArmor) {
                ArmorType.entries.map { armorType ->
                    ItemStack.hashItemAndComponents(
                        playerInventory.getItem(armorType.equippedSlot.inventoryIndex)
                    )
                }
            } else {
                emptyList()
            }
        )
    }

    @JvmStatic
    fun renderSlot(
        menu: AbstractContainerMenu,
        graphics: GuiGraphicsExtractor,
        slot: Slot
    ) {
        val state = menuState?.takeIf { it.menu === menu } ?: return
        val currentHash = ItemStack.hashItemAndComponents(slot.item)
        if (state.snapshot.slotHashes[slot.index] != currentHash) return

        state.highlightedSlots[slot.index]?.let { highlight ->
            ItemRender.drawOutline(
                graphics,
                slot.x,
                slot.y,
                highlight.outline,
                highlight.color
            )
        }
        state.itemOverlays[slot.index]?.let { overlay ->
            ItemRender.drawTextBox(
                graphics,
                slot.x,
                slot.y,
                overlay.topLines,
                TEXT_SCALE,
                TEXT_BACKGROUND_COLOR
            )
            overlay.profileText?.let { profileText ->
                ItemRender.drawTextBox(
                    graphics,
                    slot.x,
                    slot.y + PROFILE_TEXT_Y_OFFSET,
                    listOf(profileText),
                    TEXT_SCALE,
                    TEXT_BACKGROUND_COLOR
                )
            }
        }
    }

    private fun findItemOverlays(menu: AbstractContainerMenu): Map<Int, ItemOverlay> {
        return menu.slots.mapNotNull { slot ->
            val stack = slot.item.takeUnless(ItemStack::isEmpty) ?: return@mapNotNull null
            val data = RavengardItemData(stack)
            if (data.price == null && data.healing == null && data.profiles.isEmpty()) {
                return@mapNotNull null
            }

            slot.index to ItemOverlay(
                topLines = topLines(data),
                profileText = profileText(data.profiles)
            )
        }.toMap()
    }

    private fun topLines(data: RavengardItemData): List<Component> {
        val healing = data.healing
        if (healing != null) {
            return buildList {
                add(
                    Component.literal(formatNumber(healing))
                        .withColor(HEALING_COLOR and 0xFFFFFF)
                )
                data.healingDurationSeconds?.let { duration ->
                    add(
                        Component.literal("${formatNumber(duration)}s")
                            .withColor(HEALING_DURATION_COLOR and 0xFFFFFF)
                    )
                }
            }
        }

        return data.price?.let { price ->
            listOf(Component.literal(price.toString()).withColor(PRICE_COLOR and 0xFFFFFF))
        }.orEmpty()
    }

    private fun profileText(profiles: Set<Profile>): Component? {
        val matchingProfiles = Profile.entries.filter(profiles::contains)
        if (matchingProfiles.isEmpty()) return null

        return Component.empty().also { text ->
            matchingProfiles.forEach { profile ->
                text.append(
                    Component.literal(profile.displayName.first().toString())
                        .withColor(PROFILE_COLORS.getValue(profile) and 0xFFFFFF)
                )
            }
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    private fun findHighlightedSlots(
        inventory: RavengardInventory,
        equippedArmor: Map<ArmorType, RavengardArmor>
    ): Map<Int, ItemHighlight> {
        return (inventory.bestItems() + inventory.consumables())
            .filter { isBetterThanEquippedArmor(it.item, equippedArmor) }
            .associate {
                it.menuSlotIndex to ItemHighlight(
                    color = colorOf(it.item),
                    outline = ItemRender.outlineOf(it.item.stack)
                )
            }
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
        val profile: Profile?,
        val snapshot: MenuSnapshot,
        val highlightedSlots: Map<Int, ItemHighlight>,
        val itemOverlays: Map<Int, ItemOverlay>
    )

    private data class MenuSnapshot(
        val slotHashes: Map<Int, Int>,
        val equippedArmorHashes: List<Int>
    )

    private class ItemHighlight(
        val color: Int,
        val outline: List<ItemRender.OutlineSpan>
    )

    private class ItemOverlay(
        val topLines: List<Component>,
        val profileText: Component?
    )
}
