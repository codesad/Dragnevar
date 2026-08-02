package sh.stefan.dragnevar.feature.dev

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import sh.stefan.dragnevar.feature.CommandFeature
import sh.stefan.dragnevar.feature.Feature

object ItemLoreCommand : Feature(), CommandFeature {
    override fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommands.literal("itemlore").executes { context ->
                copyHeldItemLore(context.source)
            }
        )
    }

    private fun copyHeldItemLore(source: FabricClientCommandSource): Int {
        val stack = source.player.mainHandItem
        if (stack.isEmpty) {
            source.sendError(Component.literal("You aren't holding an item."))
            return 0
        }

        val lore = stack.get(DataComponents.LORE)
            ?.lines()
            ?.joinToString("\n") { it.string }
            .orEmpty()

        if (lore.isEmpty()) {
            source.sendError(Component.literal("That item has no lore."))
            return 0
        }

        source.client.keyboardHandler.clipboard = lore
        println(lore)
        source.sendFeedback(Component.literal("Copied item lore to clipboard."))
        return 1
    }
}
