package sh.stefan.dragnevar.waypoint

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

data class Waypoint(
    val senderId: String,
    val senderName: String,
    val dimension: String,
    val position: BlockPos,
    val itemName: Component?,
    val expiresAt: Long
)
