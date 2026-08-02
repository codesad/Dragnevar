package sh.stefan.dragnevar.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.network.chat.Component
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil

object ItemRender {
    private const val ITEM_SIZE = 16
    private const val OUTLINE_RADIUS = 2
    private const val TEXT_OFFSET = 1

    fun outlineOf(stack: ItemStack): List<OutlineSpan> {
        val minecraft = Minecraft.getInstance()
        val renderState = ItemStackRenderState()
        minecraft.itemModelResolver.updateForTopItem(
            renderState,
            stack,
            ItemDisplayContext.GUI,
            minecraft.level,
            minecraft.player,
            0
        )

        val contents = renderState
            .pickParticleMaterial(RandomSource.create(0L))
            ?.sprite()
            ?.contents()
            ?: return emptyList()
        val itemPixels = BooleanArray(ITEM_SIZE * ITEM_SIZE) { index ->
            val x = index % ITEM_SIZE
            val y = index / ITEM_SIZE
            contents.hasPixelIn(x, y)
        }

        return buildList {
            for (y in -OUTLINE_RADIUS until ITEM_SIZE + OUTLINE_RADIUS) {
                var spanStart: Int? = null
                for (x in -OUTLINE_RADIUS..ITEM_SIZE + OUTLINE_RADIUS) {
                    val outlined = x < ITEM_SIZE + OUTLINE_RADIUS &&
                        !itemPixels.isSet(x, y) &&
                        itemPixels.hasNeighbor(x, y)

                    if (outlined && spanStart == null) {
                        spanStart = x
                    } else if (!outlined && spanStart != null) {
                        add(OutlineSpan(spanStart, x, y))
                        spanStart = null
                    }
                }
            }
        }
    }

    fun drawOutline(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        spans: List<OutlineSpan>,
        color: Int
    ) {
        spans.forEach { span ->
            graphics.fill(
                x + span.startX,
                y + span.y,
                x + span.endX,
                y + span.y + 1,
                color
            )
        }
    }

    fun drawTextBox(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        lines: List<Component>,
        scale: Float,
        backgroundColor: Int,
        shadow: Boolean = true
    ) {
        if (lines.isEmpty()) return

        val font = Minecraft.getInstance().font
        lines.forEachIndexed { index, line ->
            val lineY = TEXT_OFFSET + font.lineHeight * index
            graphics.fill(
                x + (TEXT_OFFSET * scale).toInt(),
                y + (lineY * scale).toInt(),
                x + ceil((TEXT_OFFSET + font.width(line)) * scale).toInt(),
                y + ceil((lineY + font.lineHeight) * scale).toInt(),
                backgroundColor
            )
        }

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(scale, scale)
        lines.forEachIndexed { index, line ->
            graphics.text(
                font,
                line,
                TEXT_OFFSET,
                TEXT_OFFSET + font.lineHeight * index,
                0xFFFFFFFF.toInt(),
                shadow
            )
        }
        pose.popMatrix()
    }

    private fun SpriteContents.hasPixelIn(itemX: Int, itemY: Int): Boolean {
        val startX = itemX * width() / ITEM_SIZE
        val endX = maxOf(startX + 1, (itemX + 1) * width() / ITEM_SIZE)
        val startY = itemY * height() / ITEM_SIZE
        val endY = maxOf(startY + 1, (itemY + 1) * height() / ITEM_SIZE)

        return (startY until endY).any { textureY ->
            (startX until endX).any { textureX ->
                !isTransparent(0, textureX, textureY)
            }
        }
    }

    private fun BooleanArray.isSet(x: Int, y: Int): Boolean {
        return x in 0 until ITEM_SIZE &&
            y in 0 until ITEM_SIZE &&
            this[y * ITEM_SIZE + x]
    }

    private fun BooleanArray.hasNeighbor(x: Int, y: Int): Boolean {
        return (-OUTLINE_RADIUS..OUTLINE_RADIUS).any { offsetY ->
            (-OUTLINE_RADIUS..OUTLINE_RADIUS).any { offsetX ->
                offsetX * offsetX + offsetY * offsetY <= OUTLINE_RADIUS * OUTLINE_RADIUS &&
                    isSet(x + offsetX, y + offsetY)
            }
        }
    }

    data class OutlineSpan(val startX: Int, val endX: Int, val y: Int)
}
