package sh.stefan.dragnevar.feature.dev

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import sh.stefan.dragnevar.feature.CommandFeature
import sh.stefan.dragnevar.feature.Feature
import sh.stefan.dragnevar.ravengard.RavengardDetector
import sh.stefan.dragnevar.utils.Chat

object RavengardCheckCommand : Feature(), CommandFeature {
    override fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommands.literal("isravengard").executes { context ->
                val status = if (RavengardDetector.isOnRavengard()) {
                    "&aDetected"
                } else {
                    "&cNot detected"
                }
                Chat.sendPrefixMessage("&fRavengard: $status")
                1
            }
        )
    }
}
