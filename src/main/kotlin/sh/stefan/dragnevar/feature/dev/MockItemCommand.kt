package sh.stefan.dragnevar.feature.dev

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import sh.stefan.dragnevar.feature.CommandFeature
import sh.stefan.dragnevar.feature.Feature
import sh.stefan.dragnevar.ravengard.ProfileLabel
import sh.stefan.dragnevar.ravengard.Rarity
import sh.stefan.dragnevar.ravengard.item.type.AccessoryType
import sh.stefan.dragnevar.ravengard.item.type.ArmorType
import sh.stefan.dragnevar.ravengard.item.type.ConsumableType
import sh.stefan.dragnevar.ravengard.item.type.WeaponType
import sh.stefan.dragnevar.utils.Chat

object MockItemCommand : Feature(), CommandFeature {
    override fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommands.literal("mockitem")
                .then(giveSubcommand("armor", ::armor))
                .then(giveSubcommand("weapon", ::weapon))
                .then(giveSubcommand("accessory", ::accessory))
                .then(giveSubcommand("consumable", ::consumable))
        )
    }

    private fun giveSubcommand(name: String, createStack: () -> ItemStack) =
        ClientCommands.literal(name).executes { context ->
            give(context.source, createStack())
        }

    private fun give(source: FabricClientCommandSource, stack: ItemStack): Int {
        val gameMode = source.client.gameMode
        if (gameMode == null || !gameMode.playerMode.isCreative) {
            Chat.sendPrefixMessage("&cYou must be in creative mode.")
            return 0
        }

        val inventory = source.player.inventory
        val inventorySlot = inventory.freeSlot
        if (inventorySlot == Inventory.NOT_FOUND_INDEX) {
            Chat.sendPrefixMessage("&cYour inventory is full.")
            return 0
        }

        inventory.setItem(inventorySlot, stack)
        val menuSlot = if (Inventory.isHotbarSlot(inventorySlot)) {
            inventorySlot + 36
        } else {
            inventorySlot
        }
        gameMode.handleCreativeModeItemAdd(stack, menuSlot)
        Chat.sendPrefixMessage("&aAdded ${stack.hoverName.string}.")
        return 1
    }

    private fun armor(): ItemStack = mockStack(
        Items.DIAMOND_CHESTPLATE,
        "Mock Armor",
        "${Rarity.LEGENDARY.character} ${ArmorType.CHESTPLATE.character}",
        ProfileLabel.KNIGHT.character.toString(),
        "+100 Defense",
        "1,000 Crowns"
    )

    private fun weapon(): ItemStack = mockStack(
        Items.DIAMOND_SWORD,
        "Mock Weapon",
        "${Rarity.LEGENDARY.character} ${WeaponType.SWORD.character}",
        ProfileLabel.WARRIOR.character.toString(),
        "+25 Damage",
        "+1.5 Attack Speed",
        "2,000 Crowns"
    )

    private fun accessory(): ItemStack = mockStack(
        Items.GOLD_NUGGET,
        "Mock Accessory",
        "${Rarity.LEGENDARY.character} ${AccessoryType.RING.character}",
        ProfileLabel.ASSASSIN.character.toString(),
        "3,000 Crowns"
    )

    private fun consumable(): ItemStack = mockStack(
        Items.POTION,
        ConsumableType.HEALTH_POTION.displayName,
        Rarity.LEGENDARY.character.toString(),
        ProfileLabel.HUNTER.character.toString(),
        "Heals: +50 HP over 5 seconds",
        "500 Crowns"
    )

    private fun mockStack(item: Item, name: String, vararg lore: String): ItemStack =
        ItemStack(item).apply {
            set(DataComponents.CUSTOM_NAME, Component.literal(name))
            set(
                DataComponents.LORE,
                ItemLore(lore.map(Component::literal))
            )
        }
}
