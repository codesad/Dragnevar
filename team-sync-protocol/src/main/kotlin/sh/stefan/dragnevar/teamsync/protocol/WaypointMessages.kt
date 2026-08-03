package sh.stefan.dragnevar.teamsync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
@SerialName("ping")
data class WaypointPingRequest(
    val dimension: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val itemName: JsonElement? = null
) : ClientMessage

@Serializable
@SerialName("ping")
data class WaypointPingMessage(
    val senderId: String,
    val senderName: String,
    val dimension: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val itemName: JsonElement? = null
) : ServerMessage
