package sh.stefan.dragnevar.waypoint

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletionStage

class WaypointSocket(
    private val onWaypoint: (Waypoint) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val httpClient = HttpClient.newHttpClient()

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var connectionId = 0L

    val isConnected: Boolean
        get() = socket != null

    fun connect(url: String, room: String, playerId: String, playerName: String) {
        disconnect()
        val id = ++connectionId
        val join = JoinRequest(room, playerId, playerName)

        httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(url), ConnectionListener(id, join))
            .exceptionally { error ->
                if (connectionId == id) {
                    onStatus(
                        "Waypoint connection failed: ${error.cause?.message ?: error.message}"
                    )
                }
                null
            }
    }

    fun disconnect() {
        connectionId++
        socket?.sendClose(WebSocket.NORMAL_CLOSURE, "disconnect")
        socket = null
    }

    fun sendPing(position: BlockPos, dimension: String, itemName: Component?): Boolean {
        val activeSocket = socket ?: return false
        val message = JsonObject().apply {
            addProperty("type", "ping")
            addProperty("dimension", dimension)
            addProperty("x", position.x)
            addProperty("y", position.y)
            addProperty("z", position.z)
            itemName?.let {
                ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, it)
                    .result()
                    .ifPresent { encoded -> add("itemName", encoded) }
            }
        }
        activeSocket.sendText(message.toString(), true)
        return true
    }

    private fun handleMessage(rawMessage: String) {
        val message = runCatching { JsonParser.parseString(rawMessage).asJsonObject }
            .getOrNull() ?: return

        when (message.string("type")) {
            "joined" -> onStatus("Joined waypoint room ${message.string("room")}")
            "error" -> onStatus("Waypoint server: ${message.string("message")}")
            "ping" -> parseWaypoint(message)?.let(onWaypoint)
        }
    }

    private fun parseWaypoint(message: JsonObject): Waypoint? {
        return runCatching {
            Waypoint(
                senderId = message.string("senderId"),
                senderName = message.string("senderName"),
                dimension = message.string("dimension"),
                position = BlockPos(
                    message.get("x").asInt,
                    message.get("y").asInt,
                    message.get("z").asInt
                ),
                itemName = message.get("itemName")?.let {
                    ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, it)
                        .result()
                        .orElse(null)
                },
                expiresAt = 0
            )
        }.getOrNull()
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
                addProperty("room", join.room)
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
                onStatus("Waypoint connection closed$detail")
            }
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            if (connectionId == id) {
                socket = null
                onStatus(
                    "Waypoint connection error: ${error.message ?: error.javaClass.simpleName}"
                )
            }
        }
    }

    private data class JoinRequest(
        val room: String,
        val playerId: String,
        val playerName: String
    )
}
