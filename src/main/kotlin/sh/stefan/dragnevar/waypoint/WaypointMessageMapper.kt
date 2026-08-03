package sh.stefan.dragnevar.waypoint

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import kotlinx.serialization.json.Json
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import sh.stefan.dragnevar.teamsync.protocol.WaypointPingMessage
import sh.stefan.dragnevar.teamsync.protocol.WaypointPingRequest

object WaypointMessageMapper {
    fun toRequest(
        position: BlockPos,
        dimension: String,
        itemName: Component?
    ) = WaypointPingRequest(
        dimension = dimension,
        x = position.x,
        y = position.y,
        z = position.z,
        itemName = itemName?.let(::encodeComponent)
    )

    fun toWaypoint(message: WaypointPingMessage) = Waypoint(
        senderId = message.senderId,
        senderName = message.senderName,
        dimension = message.dimension,
        position = BlockPos(message.x, message.y, message.z),
        itemName = message.itemName?.let(::decodeComponent),
        expiresAt = 0
    )

    private fun encodeComponent(component: Component) =
        ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component)
            .result()
            .map { Json.parseToJsonElement(it.toString()) }
            .orElse(null)

    private fun decodeComponent(component: kotlinx.serialization.json.JsonElement) =
        ComponentSerialization.CODEC.parse(
            JsonOps.INSTANCE,
            JsonParser.parseString(component.toString())
        ).result().orElse(null)
}
