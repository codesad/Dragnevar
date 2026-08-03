package sh.stefan.dragnevar.teamsync.protocol

import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import sh.stefan.dragnevar.waypoint.Waypoint

object WaypointMessages {
    const val MESSAGE_TYPE = "ping"

    fun createPing(
        position: BlockPos,
        dimension: String,
        itemName: Component?
    ) = JsonObject().apply {
        addProperty("type", MESSAGE_TYPE)
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

    fun parse(message: JsonObject): Waypoint? = runCatching {
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

    private fun JsonObject.string(name: String): String = get(name).asString
}
