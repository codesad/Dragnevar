package sh.stefan.dragnevar.teamsync

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletionStage

class TeamSyncSocket(
    private val onMessage: (JsonObject) -> Unit,
    private val onStatus: (TeamSyncConnectionState) -> Unit,
    private val onServerVersion: (String) -> Unit
) {
    private val httpClient = HttpClient.newHttpClient()

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var connectionId = 0L

    val isConnected: Boolean
        get() = socket != null

    fun connect(url: String, teamCode: String, playerId: String, playerName: String) {
        disconnect()
        onStatus(TeamSyncConnectionState.Connecting)
        val id = ++connectionId
        val join = JoinRequest(teamCode, playerId, playerName)

        httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(url), ConnectionListener(id, join))
            .exceptionally { error ->
                if (connectionId == id) {
                    onStatus(
                        TeamSyncConnectionState.Error(
                            error.cause?.message ?: error.message ?: "Could not connect."
                        )
                    )
                }
                null
            }
    }

    fun disconnect() {
        connectionId++
        socket?.sendClose(WebSocket.NORMAL_CLOSURE, "disconnect")
        socket = null
        onStatus(TeamSyncConnectionState.Disconnected)
    }

    fun send(message: JsonObject): Boolean {
        val activeSocket = socket ?: return false
        activeSocket.sendText(message.toString(), true)
        return true
    }

    private fun handleMessage(rawMessage: String) {
        val message = runCatching { JsonParser.parseString(rawMessage).asJsonObject }
            .getOrNull() ?: return

        when (message.string("type")) {
            "joined" -> {
                onStatus(TeamSyncConnectionState.Connected)
                message.get("version")?.asString?.let(onServerVersion)
            }
            "error" -> onStatus(TeamSyncConnectionState.Error(message.string("message")))
            else -> onMessage(message)
        }
    }

    private fun JsonObject.string(name: String): String = get(name).asString

    private inner class ConnectionListener(
        private val id: Long,
        private val join: JoinRequest
    ) : WebSocket.Listener {
        private val messageBuffer = StringBuilder()

        override fun onOpen(webSocket: WebSocket) {
            if (connectionId != id) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "cancelled")
                return
            }

            socket = webSocket
            val message = JsonObject().apply {
                addProperty("type", "join")
                addProperty("teamCode", join.teamCode)
                addProperty("playerId", join.playerId)
                addProperty("playerName", join.playerName)
            }
            webSocket.sendText(message.toString(), true)
            webSocket.request(1)
        }

        override fun onText(
            webSocket: WebSocket,
            data: CharSequence,
            last: Boolean
        ): CompletionStage<*>? {
            if (connectionId != id) return null

            messageBuffer.append(data)
            if (last) {
                handleMessage(messageBuffer.toString())
                messageBuffer.setLength(0)
            }
            webSocket.request(1)
            return null
        }

        override fun onClose(
            webSocket: WebSocket,
            statusCode: Int,
            reason: String
        ): CompletionStage<*>? {
            if (connectionId == id) {
                socket = null
                val detail = reason.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()
                onStatus(TeamSyncConnectionState.Error("Connection closed$detail"))
            }
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            if (connectionId == id) {
                socket = null
                onStatus(
                    TeamSyncConnectionState.Error(
                        error.message ?: error.javaClass.simpleName
                    )
                )
            }
        }
    }

    private data class JoinRequest(
        val teamCode: String,
        val playerId: String,
        val playerName: String
    )
}
