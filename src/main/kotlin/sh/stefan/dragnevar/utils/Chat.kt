package sh.stefan.dragnevar.utils

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ChatGradient {
    data class Gradient(val start: Int, val end: Int)

    private val colors = mapOf(
        'f' to Gradient(0xCFE8FF, 0xFFFFFF),
        'e' to Gradient(0xFFB52E, 0xFFF96A),
        '6' to Gradient(0xFF7A1A, 0xFFB52E),
        'a' to Gradient(0x18D95E, 0x75FF75),
        'b' to Gradient(0x2C9CFF, 0x55FFFF),
        'c' to Gradient(0xFF3030, 0xFF7777),
        '9' to Gradient(0x5577FF, 0x66AAFF),
        '7' to Gradient(0x777777, 0xAAAAAA),
        '8' to Gradient(0x444444, 0x777777)
    )

    private val defaultGradient = colors.getValue('f')

    fun parse(message: String): Component {
        val out = Component.empty()
        val segment = StringBuilder()
        var gradient = defaultGradient
        var i = 0

        fun flush() {
            if (segment.isEmpty()) return
            out.append(gradient(segment.toString(), gradient))
            segment.clear()
        }

        while (i < message.length) {
            if (message[i] == '&' && i + 1 < message.length) {
                val code = message[i + 1].lowercaseChar()

                if (code == 'r') {
                    flush()
                    gradient = defaultGradient
                    i += 2
                    continue
                }

                val next = colors[code]
                if (next != null) {
                    flush()
                    gradient = next
                    i += 2
                    continue
                }
            }

            segment.append(message[i])
            i++
        }

        flush()
        return out
    }

    fun gradient(text: String, gradient: Gradient): Component {
        val out = Component.empty()
        val count = text.count { !it.isWhitespace() }.coerceAtLeast(1)
        var index = 0

        for (char in text) {
            val t = if (count <= 1) 0.0 else index.toDouble() / (count - 1)
            val color = lerpColor(gradient.start, gradient.end, t)

            out.append(
                Component.literal(char.toString()).withStyle {
                    it.withColor(color)
                }
            )

            if (!char.isWhitespace()) index++
        }

        return out
    }

    private fun lerpColor(start: Int, end: Int, t: Double): Int {
        val sr = start shr 16 and 0xFF
        val sg = start shr 8 and 0xFF
        val sb = start and 0xFF

        val er = end shr 16 and 0xFF
        val eg = end shr 8 and 0xFF
        val eb = end and 0xFF

        val r = (sr + (er - sr) * t).toInt()
        val g = (sg + (eg - sg) * t).toInt()
        val b = (sb + (eb - sb) * t).toInt()

        return (r shl 16) or (g shl 8) or b
    }
}


object Chat {
    private val prefixGradient = ChatGradient.Gradient(0x55FFFF, 0x5577FF)
    private val plainPrefix = ChatGradient.gradient("ᴅʀᴀɢɴᴇᴠᴀʀ \u00BB ", prefixGradient)

    fun stripFormatting(value: String): String =
        ChatFormatting.stripFormatting(value) ?: value

    @JvmStatic
    @JvmOverloads
    fun sendPrefixMessage(
        message: String,
        color: Boolean = true,
        prefix: Component = plainPrefix
    ) {
        val text = prefix.copy().append(
            if (color) ChatGradient.parse(message)
            else Component.literal(message)
        )

        val client = Minecraft.getInstance()
        client.execute {
            client.player?.sendSystemMessage(text)
        }
    }
}
