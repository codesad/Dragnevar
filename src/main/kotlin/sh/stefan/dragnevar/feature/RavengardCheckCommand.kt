package sh.stefan.dragnevar.feature

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component
import sh.stefan.dragnevar.ravengard.RavengardDetector

object RavengardCheckCommand : Feature(), CommandFeature {
    override fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommands.literal("isravengard").executes { context ->
                val status = if (RavengardDetector.isOnRavengard()) "yes" else "no"
                context.source.sendFeedback(Component.literal("Ravengard: $status"))
                1
            }
        )
    }
}
