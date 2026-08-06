package sh.stefan.dragnevar.feature.dev

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import sh.stefan.dragnevar.feature.ClassDetector
import sh.stefan.dragnevar.feature.CommandFeature
import sh.stefan.dragnevar.feature.Feature
import sh.stefan.dragnevar.ravengard.Profile
import sh.stefan.dragnevar.utils.Chat

object SetClassCommand : Feature(), CommandFeature {
    override fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        val command = ClientCommands.literal("setclass")
        Profile.entries.forEach { profile ->
            command.then(
                ClientCommands.literal(profile.name.lowercase())
                    .executes {
                        ClassDetector.setProfile(profile)
                        Chat.sendPrefixMessage("&aSet your class to &b${profile.displayName}&a.")
                        1
                    }
            )
        }
        dispatcher.register(command)
    }
}
