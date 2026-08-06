package sh.stefan.dragnevar.feature.dev

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.SharedSuggestionProvider
import sh.stefan.dragnevar.feature.CommandFeature
import sh.stefan.dragnevar.feature.Feature
import sh.stefan.dragnevar.teamsync.HypixelPartyProvider
import sh.stefan.dragnevar.utils.Chat

object FakePartyMemberCommand : Feature(), CommandFeature {
    override fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommands.literal("fakeparty")
                .then(
                    ClientCommands.literal("add")
                        .then(playerArgument(::addFakeMember))
                )
                .then(
                    ClientCommands.literal("remove")
                        .then(playerArgument(::removeFakeMember))
                )
                .then(
                    ClientCommands.literal("clear")
                        .executes {
                            HypixelPartyProvider.clearFakeMembers()
                            Chat.sendPrefixMessage("&aCleared fake party members.")
                            1
                        }
                )
        )
    }

    private fun playerArgument(action: (FabricClientCommandSource, String) -> Int) =
        ClientCommands.argument("username", StringArgumentType.word())
            .suggests { context, builder ->
                SharedSuggestionProvider.suggest(
                    context.source.onlinePlayerNames,
                    builder
                )
            }
            .executes { context ->
                action(
                    context.source,
                    StringArgumentType.getString(context, "username")
                )
            }

    private fun addFakeMember(source: FabricClientCommandSource, username: String): Int {
        val profile = source.client.connection
            ?.getPlayerInfoIgnoreCase(username)
            ?.profile

        if (profile == null) {
            Chat.sendPrefixMessage("&cCouldn't find $username.")
            return 0
        }

        HypixelPartyProvider.addFakeMember(profile.id)
        Chat.sendPrefixMessage("&aAdded ${profile.name} as a fake party member.")
        return 1
    }

    private fun removeFakeMember(source: FabricClientCommandSource, username: String): Int {
        val profile = source.client.connection
            ?.getPlayerInfoIgnoreCase(username)
            ?.profile

        if (profile == null) {
            Chat.sendPrefixMessage("&cCouldn't find $username.")
            return 0
        }

        HypixelPartyProvider.removeFakeMember(profile.id)
        Chat.sendPrefixMessage("&aRemoved ${profile.name} from the fake party members.")
        return 1
    }
}
