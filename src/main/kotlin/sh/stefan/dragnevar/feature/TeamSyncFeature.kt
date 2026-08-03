package sh.stefan.dragnevar.feature

import com.google.gson.JsonObject
import net.fabricmc.loader.api.Version
import net.minecraft.client.Minecraft
import sh.stefan.dragnevar.Dragnevar
import sh.stefan.dragnevar.config.DragnevarConfig
import sh.stefan.dragnevar.teamsync.TeamSyncConnectionState
import sh.stefan.dragnevar.teamsync.TeamMember
import sh.stefan.dragnevar.teamsync.TeamSyncSocket
import sh.stefan.dragnevar.teamsync.protocol.TeamMessages
import sh.stefan.dragnevar.utils.Chat
import java.util.concurrent.ConcurrentHashMap

object TeamSyncFeature : Feature(), WorldConnectionFeature, TickFeature {
    private const val MESSAGE_PREFIX = "&b[TS] &r"
    private val messageHandlers = ConcurrentHashMap<String, (JsonObject) -> Unit>()
    private val teamMembers = ConcurrentHashMap<String, TeamMember>()
    private val socket = TeamSyncSocket(
        ::receiveMessage,
        ::showStatus,
        ::checkServerVersion
    )
    private var wasEnabled = DragnevarConfig.values.teamSync.enabled

    @Volatile
    var connectionState: TeamSyncConnectionState = TeamSyncConnectionState.Disconnected
        private set

    val isConnected: Boolean
        get() = socket.isConnected

    private val hasActiveConnection: Boolean
        get() = isConnected || connectionState is TeamSyncConnectionState.Connecting

    val members: List<TeamMember>
        get() = teamMembers.values.sortedBy(TeamMember::playerName)

    override fun onWorldJoin() {
        if (!DragnevarConfig.values.teamSync.enabled) return
        if (hasActiveConnection) return

        val config = DragnevarConfig.values.teamSync.connection
        if (config.websocketUrl.isBlank() || config.teamCode.isBlank()) return

        connectConfigured().onFailure(::showConnectionError)
    }

    override fun onTick() {
        val enabled = DragnevarConfig.values.teamSync.enabled
        if (enabled == wasEnabled) return

        wasEnabled = enabled
        if (!enabled) {
            disconnect()
        } else if (Minecraft.getInstance().level != null) {
            onWorldJoin()
        }
    }

    fun registerMessageHandler(type: String, handler: (JsonObject) -> Unit) {
        check(messageHandlers.putIfAbsent(type, handler) == null) {
            "A Team Sync handler is already registered for $type"
        }
    }

    fun send(message: JsonObject): Boolean =
        DragnevarConfig.values.teamSync.enabled && socket.send(message)

    fun sendPrefixMessage(message: String) {
        Chat.sendPrefixMessage(MESSAGE_PREFIX + message)
    }

    fun toggleConnection() {
        if (hasActiveConnection) {
            disconnect()
            return
        }
        if (!DragnevarConfig.values.teamSync.enabled) {
            sendPrefixMessage("&eEnable Team Sync first.")
            return
        }

        DragnevarConfig.save()
        connectConfigured().onFailure(::showConnectionError)
    }

    private fun connectConfigured(): Result<Unit> {
        val config = DragnevarConfig.values.teamSync.connection
        return connect(config.websocketUrl, config.teamCode)
    }

    private fun connect(url: String, teamCode: String): Result<Unit> = runCatching {
        require(url.isNotBlank()) { "WebSocket URL cannot be empty." }
        require(teamCode.isNotBlank()) { "Team code cannot be empty." }
        val player = Minecraft.getInstance().player
            ?: error("Join a world before connecting.")
        socket.connect(url, teamCode, player.stringUUID, player.name.string)
    }

    private fun disconnect() {
        socket.disconnect()
        teamMembers.clear()
    }

    private fun receiveMessage(message: JsonObject) {
        if (!DragnevarConfig.values.teamSync.enabled) return

        val type = message.get("type")?.asString ?: return
        when (type) {
            TeamMessages.JOINED -> receiveTeamMembers(message)
            TeamMessages.MEMBER_JOINED -> TeamMessages.member(message)?.let { member ->
                teamMembers[member.playerId] = member
                showTeamMessage("&a${member.playerName} joined.")
            }
            TeamMessages.MEMBER_LEFT -> {
                message.get("playerId")?.asString
                    ?.let(teamMembers::remove)
                    ?.let { member ->
                        showTeamMessage("&c${member.playerName} left.")
                    }
            }
            else -> messageHandlers[type]?.invoke(message)
        }
    }

    private fun receiveTeamMembers(message: JsonObject) {
        val existingMembers = TeamMessages.members(message)
            .sortedBy(TeamMember::playerName)
        teamMembers.clear()
        existingMembers.associateByTo(teamMembers, TeamMember::playerId)

        val text = if (existingMembers.isEmpty()) {
            "&7No teammates are connected yet."
        } else {
            "&fAlready connected: &a${existingMembers.joinToString { it.playerName }}"
        }
        showTeamMessage(text)
    }

    private fun showTeamMessage(message: String) {
        sendPrefixMessage(message)
    }

    private fun showStatus(state: TeamSyncConnectionState) {
        connectionState = state
        if (state !is TeamSyncConnectionState.Error) return

        sendPrefixMessage("&c${state.message}")
    }

    private fun checkServerVersion(serverVersion: String) {
        val requiredVersion = runCatching { Version.parse(serverVersion) }
            .getOrElse {
                logger.warning("Team Sync returned an invalid version: $serverVersion")
                return
            }
        if (Dragnevar.VERSION >= requiredVersion) return

        sendPrefixMessage(
            "&eDragnevar ${Dragnevar.VERSION.friendlyString} is outdated for Team Sync. " +
                "Some features may be broken or missing. " +
                "Update to $serverVersion or newer."
        )
    }

    private fun showConnectionError(error: Throwable) {
        val message = error.message ?: "Could not connect."
        connectionState = TeamSyncConnectionState.Error(message)
        sendPrefixMessage("&c$message")
    }
}
