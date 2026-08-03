package sh.stefan.dragnevar.teamsync.protocol

import kotlinx.serialization.json.Json

object TeamSyncProtocol {
    private val json = Json {
        classDiscriminator = "type"
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    fun encode(message: ClientMessage): String =
        json.encodeToString(ClientMessage.serializer(), message)

    fun encode(message: ServerMessage): String =
        json.encodeToString(ServerMessage.serializer(), message)

    fun decodeClient(message: String): ClientMessage =
        json.decodeFromString(ClientMessage.serializer(), message)

    fun decodeServer(message: String): ServerMessage =
        json.decodeFromString(ServerMessage.serializer(), message)
}
