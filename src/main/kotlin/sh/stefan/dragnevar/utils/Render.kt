package sh.stefan.dragnevar.utils

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.util.ARGB
import net.minecraft.world.phys.Vec3
import org.joml.Vector4f
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

object Render {
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
