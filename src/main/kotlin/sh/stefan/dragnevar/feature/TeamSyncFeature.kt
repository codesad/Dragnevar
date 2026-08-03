package sh.stefan.dragnevar.feature

import com.google.gson.JsonObject
import net.fabricmc.loader.api.Version
import net.minecraft.client.Minecraft
import sh.stefan.dragnevar.Dragnevar
import sh.stefan.dragnevar.config.DragnevarConfig
import sh.stefan.dragnevar.teamsync.TeamSyncConnectionState
import sh.stefan.dragnevar.teamsync.TeamSyncSocket
import sh.stefan.dragnevar.utils.Chat
import java.util.concurrent.ConcurrentHashMap

object TeamSyncFeature : Feature(), WorldConnectionFeature {
    private val messageHandlers = ConcurrentHashMap<String, (JsonObject) -> Unit>()
    private val socket = TeamSyncSocket(
        ::receiveMessage,
        ::showStatus,
        ::checkServerVersion
    )

    @Volatile
    var connectionState: TeamSyncConnectionState = TeamSyncConnectionState.Disconnected
        private set

    val isConnected: Boolean
        get() = socket.isConnected

    override fun onWorldJoin() {
        val config = DragnevarConfig.values.teamSync.connection
        if (config.websocketUrl.isBlank() || config.teamCode.isBlank()) return

        connectConfigured().onFailure(::showConnectionError)
    }

    override fun onWorldLeave() {
        disconnect()
    }

    fun registerMessageHandler(type: String, handler: (JsonObject) -> Unit) {
        check(messageHandlers.putIfAbsent(type, handler) == null) {
            "A Team Sync handler is already registered for $type"
        }
    }

    fun send(message: JsonObject): Boolean = socket.send(message)

    fun toggleConnection() {
        if (isConnected || connectionState is TeamSyncConnectionState.Connecting) {
            disconnect()
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
    }

    private fun receiveMessage(message: JsonObject) {
        val type = message.get("type")?.asString ?: return
        messageHandlers[type]?.invoke(message)
    }

    private fun showStatus(state: TeamSyncConnectionState) {
        connectionState = state
        if (state !is TeamSyncConnectionState.Error) return

        val client = Minecraft.getInstance()
        client.execute {
            client.player?.let { player ->
                Chat.showError(player, state.message)
            }
        }
    }

    private fun checkServerVersion(serverVersion: String) {
        val requiredVersion = runCatching { Version.parse(serverVersion) }
            .getOrElse {
                logger.warning("Team Sync returned an invalid version: $serverVersion")
                return
            }
        if (Dragnevar.VERSION >= requiredVersion) return

        val client = Minecraft.getInstance()
        client.execute {
            client.player?.let { player ->
                Chat.showError(
                    player,
                    "Dragnevar ${Dragnevar.VERSION.friendlyString} is outdated for Team Sync. " +
                        "Some features may be broken or missing. " +
                        "Update to $serverVersion or newer."
                )
            }
        }
    }

    private fun showConnectionError(error: Throwable) {
        val message = error.message ?: "Could not connect."
        connectionState = TeamSyncConnectionState.Error(message)
        val client = Minecraft.getInstance()
        client.execute {
            client.player?.let { player ->
                Chat.showError(player, message)
            }
        }
    }
}
