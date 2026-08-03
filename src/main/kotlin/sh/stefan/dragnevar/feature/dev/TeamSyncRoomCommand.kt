package sh.stefan.dragnevar.feature.dev

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import sh.stefan.dragnevar.feature.CommandFeature
import sh.stefan.dragnevar.feature.Feature
import sh.stefan.dragnevar.feature.TeamSyncFeature
import sh.stefan.dragnevar.teamsync.ProfileNames
import sh.stefan.dragnevar.utils.Chat
import java.util.concurrent.CompletableFuture

object TeamSyncRoomCommand : Feature(), CommandFeature {
    override fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommands.literal("tsroom").executes { showRoom() }
        )
    }

    private fun showRoom(): Int {
        val room = TeamSyncFeature.roomSnapshot()
        if (room == null) {
            Chat.sendPrefixMessage("&eJoin a Hypixel party first.")
            return 0
        }

        CompletableFuture.supplyAsync {
            room.map { (id, connectedName) ->
                RoomMember(
                    connectedName ?: ProfileNames.resolve(id),
                    connectedName != null
                )
            }.sortedBy { it.name.lowercase() }
        }.thenAccept { members ->
            val names = members.joinToString("&7, ") {
                "${if (it.connected) "&9" else "&6"}${it.name}"
            }
            Chat.sendPrefixMessage("&bRoom: $names")
        }
        return 1
    }

    private data class RoomMember(
        val name: String,
        val connected: Boolean
    )
}
