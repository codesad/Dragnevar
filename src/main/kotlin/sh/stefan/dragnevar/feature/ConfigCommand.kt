package sh.stefan.dragnevar.feature

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import sh.stefan.dragnevar.config.DragnevarConfig

object ConfigCommand : Feature(), CommandFeature {
    override fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommands.literal("rgconfig").executes { context ->
                context.source.client.execute {
                    context.source.client.gui.setScreen(DragnevarConfig.createScreen(null))
                }
                1
            }
        )
    }
}
