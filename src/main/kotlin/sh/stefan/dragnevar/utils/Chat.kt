package sh.stefan.dragnevar.utils

import net.minecraft.ChatFormatting

object Chat {
    fun stripFormatting(value: String): String =
        ChatFormatting.stripFormatting(value) ?: value
}
