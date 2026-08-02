package sh.stefan.dragnevar.utils

import net.minecraft.ChatFormatting
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component

object Chat {
    fun stripFormatting(value: String): String =
        ChatFormatting.stripFormatting(value) ?: value

    fun showError(player: LocalPlayer, message: String) {
        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED))
    }
}
