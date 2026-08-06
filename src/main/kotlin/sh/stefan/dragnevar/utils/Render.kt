package sh.stefan.dragnevar.utils

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.util.ARGB
import net.minecraft.world.phys.Vec3
import org.joml.Vector4f
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

object Render {
    fun drawDashedLine(
        graphics: GuiGraphicsExtractor,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        color: Int,
        dashLength: Float = 4.0f,
        gapLength: Float = 3.0f
    ) {
        val deltaX = (endX - startX).toFloat()
        val deltaY = (endY - startY).toFloat()
        val length = hypot(deltaX, deltaY)
        if (length == 0.0f) return

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(startX.toFloat(), startY.toFloat())
        pose.rotate(atan2(deltaY, deltaX))

        var dashStart = 0.0f
        while (dashStart < length) {
            val dashEnd = min(dashStart + dashLength, length)
            val start = dashStart.roundToInt()
            val end = dashEnd.roundToInt().coerceAtLeast(start + 1)

            graphics.fill(start, 0, end, 1, color)
            dashStart += dashLength + gapLength
        }

        pose.popMatrix()
    }

    fun projectToScreen(
        graphics: GuiGraphicsExtractor,
        camera: CameraRenderState,
        target: Vec3,
        horizontalMargin: Int = 0,
        topMargin: Int = 0,
        bottomMargin: Int = 0
    ): ScreenPosition? {
        val projected = Vector4f(
            (target.x - camera.pos.x).toFloat(),
            (target.y - camera.pos.y).toFloat(),
            (target.z - camera.pos.z).toFloat(),
            1.0f
        )
        camera.viewRotationMatrix.transform(projected)
        camera.projectionMatrix.transform(projected)
        if (projected.w <= 0.0f) return null

        val screenX = ((projected.x / projected.w + 1.0f) * graphics.guiWidth() / 2.0f)
            .roundToInt()
        val screenY = ((1.0f - projected.y / projected.w) * graphics.guiHeight() / 2.0f)
            .roundToInt()

        return ScreenPosition(
            screenX.coerceIn(horizontalMargin, graphics.guiWidth() - horizontalMargin),
            screenY.coerceIn(topMargin, graphics.guiHeight() - bottomMargin)
        )
    }

    fun drawDiamond(
        graphics: GuiGraphicsExtractor,
        centerX: Int,
        centerY: Int,
        radius: Int,
        color: Int
    ) {
        for (offsetY in -radius..radius) {
            val halfWidth = radius - abs(offsetY)
            graphics.fill(
                centerX - halfWidth,
                centerY + offsetY,
                centerX + halfWidth + 1,
                centerY + offsetY + 1,
                color
            )
        }
    }

    fun locatorBarColor(playerId: String): Int {
        val playerHash = runCatching { UUID.fromString(playerId).hashCode() }
            .getOrElse { playerId.hashCode() }
        return ARGB.setBrightness(ARGB.color(255, playerHash), 0.9f)
    }

    data class ScreenPosition(val x: Int, val y: Int)
}
