package sh.stefan.dragnevar.config

import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.component.TextComponent
import io.github.notenoughupdates.moulconfig.gui.editors.ComponentEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import sh.stefan.dragnevar.config.component.ConfigButton
import sh.stefan.dragnevar.feature.WaypointFeature
import sh.stefan.dragnevar.teamsync.TeamSyncConnectionState
import java.util.function.Supplier

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class TeamSyncConnectionStatus

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class TeamSyncConnectionButton

class TeamSyncConnectionStatusEditor(option: ProcessedOption) : ComponentEditor(option) {
    private var component: GuiComponent? = null

    override fun getDelegate(): GuiComponent = component ?: wrapComponent(
        TextComponent(
            IMinecraft.INSTANCE.defaultFontRenderer,
            Supplier { StructuredText.of(WaypointFeature.connectionState.displayText) },
            100,
            TextComponent.TextAlignment.CENTER,
            false,
            false
        )
    ).also { component = it }
}

private val TeamSyncConnectionState.displayText: String
    get() = when (this) {
        TeamSyncConnectionState.Disconnected -> "§cDisconnected"
        TeamSyncConnectionState.Connecting -> "§eConnecting..."
        TeamSyncConnectionState.Connected -> "§aConnected"
        is TeamSyncConnectionState.Error -> "§c$message"
    }

class TeamSyncConnectionButtonEditor(option: ProcessedOption) : ComponentEditor(option) {
    private var component: GuiComponent? = null

    override fun getDelegate(): GuiComponent = component ?: wrapComponent(
        ConfigButton(
            width = 72,
            text = {
                if (
                    WaypointFeature.isConnected ||
                    WaypointFeature.connectionState is TeamSyncConnectionState.Connecting
                ) {
                    "Disconnect"
                } else {
                    "Connect"
                }
            },
            action = WaypointFeature::toggleConnection
        )
    ).also { component = it }
}
