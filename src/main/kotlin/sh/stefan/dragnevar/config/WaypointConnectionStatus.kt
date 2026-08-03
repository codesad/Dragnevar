package sh.stefan.dragnevar.config

import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.GuiTextures
import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.gui.component.TextComponent
import io.github.notenoughupdates.moulconfig.gui.editors.ComponentEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import sh.stefan.dragnevar.feature.WaypointFeature
import java.util.function.Supplier

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class WaypointConnectionStatus

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class WaypointConnectionButton

class WaypointConnectionStatusEditor(option: ProcessedOption) : ComponentEditor(option) {
    private var component: GuiComponent? = null

    override fun getDelegate(): GuiComponent = component ?: wrapComponent(
        TextComponent(
            IMinecraft.INSTANCE.defaultFontRenderer,
            Supplier {
                StructuredText.of(
                    if (WaypointFeature.isConnected) "§aConnected" else "§cDisconnected"
                )
            },
            100,
            TextComponent.TextAlignment.CENTER,
            false,
            false
        )
    ).also { component = it }
}

class WaypointConnectionButtonEditor(option: ProcessedOption) : ComponentEditor(option) {
    private var component: GuiComponent? = null

    override fun getDelegate(): GuiComponent = component ?: wrapComponent(
        object : GuiComponent() {
            override fun getWidth() = 72

            override fun getHeight() = 16

            override fun render(context: GuiImmediateContext) {
                val text = StructuredText.of(
                    if (WaypointFeature.isConnected) "Disconnect" else "Connect"
                )
                context.renderContext.drawTexturedRect(
                    GuiTextures.BUTTON,
                    0f,
                    0f,
                    context.width.toFloat(),
                    context.height.toFloat()
                )
                context.renderContext.drawStringCenteredScaledMaxWidth(
                    text,
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
                    WaypointFeature.toggleConnection()
                    return true
                }
                return false
            }
        }
    ).also { component = it }
}
