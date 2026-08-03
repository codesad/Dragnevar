package sh.stefan.dragnevar.feature.dev

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.core.component.DataComponents
import sh.stefan.dragnevar.feature.CommandFeature
import sh.stefan.dragnevar.feature.Feature
import sh.stefan.dragnevar.utils.Chat

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
            Chat.sendPrefixMessage("&cYou aren't holding an item.")
            return 0
        }

        val lore = stack.get(DataComponents.LORE)
            ?.lines()
            ?.joinToString("\n") { it.string }
            .orEmpty()

        if (lore.isEmpty()) {
            Chat.sendPrefixMessage("&cThat item has no lore.")
            return 0
        }

        source.client.keyboardHandler.clipboard = lore
        println(lore)
        Chat.sendPrefixMessage("&aCopied item lore to clipboard.")
        return 1
    }
}
