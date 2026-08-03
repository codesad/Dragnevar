package sh.stefan.dragnevar.config.component

import io.github.notenoughupdates.moulconfig.GuiTextures
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext
import io.github.notenoughupdates.moulconfig.gui.MouseEvent

class ConfigButton(
    private val width: Int,
    private val text: () -> String,
    private val action: () -> Unit
) : GuiComponent() {
    override fun getWidth() = width

    override fun getHeight() = 16

    override fun render(context: GuiImmediateContext) {
        context.renderContext.drawTexturedRect(
            GuiTextures.BUTTON,
            0f,
            0f,
            context.width.toFloat(),
            context.height.toFloat()
        )
        context.renderContext.drawStringCenteredScaledMaxWidth(
            StructuredText.of(text()),
            context.renderContext.minecraft.defaultFontRenderer,
            context.width / 2f,
            context.height / 2f,
            false,
            context.width - 4,
            0xFF303030.toInt()
        )
    }

    override fun mouseEvent(
        mouseEvent: MouseEvent,
        context: GuiImmediateContext
    ): Boolean {
        if (
            mouseEvent is MouseEvent.Click &&
            mouseEvent.mouseState &&
            mouseEvent.mouseButton == 0 &&
            context.isHovered
        ) {
            action()
            return true
        }
        return false
    }
}
