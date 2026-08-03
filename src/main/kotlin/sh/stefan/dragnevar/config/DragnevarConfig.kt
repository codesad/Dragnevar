package sh.stefan.dragnevar.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig
import net.fabricmc.loader.api.FabricLoader
import sh.stefan.dragnevar.Dragnevar
import java.io.File
import java.util.function.BiConsumer
import java.util.logging.Level

class DragnevarConfigData : Config() {
    @field:Expose
    @field:Category(name = "Items", desc = "Item highlighting and behavior.")
    @JvmField
    var items = ItemConfig()

    @field:Expose
    @field:Category(name = "Waypoints", desc = "Shared waypoint pings.")
    @JvmField
    var waypoints = WaypointConfig()

    override fun getTitle(): StructuredText = StructuredText.of("Dragnevar")
}

class ItemConfig {
    @field:Expose
    @field:ConfigOption(
        name = "Weapon Priority",
        desc = "DPS uses damage multiplied by attack speed. Damage prioritizes raw damage, using attack speed only to break ties."
    )
    @field:ConfigEditorDropdown
    @JvmField
    var weaponPriority = WeaponPriority.DPS

    @field:Expose
    @field:ConfigOption(
        name = "Prevent Item Drop",
        desc = "Prevents your client from dropping the held item when using an ability, avoiding the misleading visual cooldown reset."
    )
    @field:ConfigEditorBoolean
    @JvmField
    var preventRavengardItemDrop = true
}

class WaypointConfig {
    @field:Expose
    @field:ConfigOption(name = "WebSocket URL", desc = "Address of the waypoint relay server.")
    @field:ConfigEditorText
    @JvmField
    var websocketUrl = ""

    @field:Expose
    @field:ConfigOption(name = "Team Name", desc = "Players using the same team name share pings.")
    @field:ConfigEditorText
    @JvmField
    var teamName = ""

    @field:ConfigOption(name = "Status", desc = "Current waypoint server connection.")
    @field:WaypointConnectionStatus
    @Transient
    @JvmField
    var connectionStatus = Unit

    @field:ConfigOption(name = "Connection", desc = "Connects to or disconnects from the waypoint server.")
    @field:WaypointConnectionButton
    @Transient
    @JvmField
    var connectionAction = Unit

    @field:Expose
    @field:ConfigOption(name = "Waypoint Duration", desc = "How long waypoint markers remain visible, in seconds.")
    @field:ConfigEditorSlider(minValue = 5f, maxValue = 120f, minStep = 5f)
    @JvmField
    var waypointTimeoutSeconds = 30

    @field:Expose
    @field:ConfigOption(name = "Ping Sound", desc = "Plays a sound when a waypoint is received.")
    @field:ConfigEditorBoolean
    @JvmField
    var playPingSound = true

    @field:Expose
    @field:ConfigOption(name = "Show Distance", desc = "Shows the distance next to each waypoint.")
    @field:ConfigEditorBoolean
    @JvmField
    var showWaypointDistance = true
}

object DragnevarConfig {
    private val configFile: File
        get() = FabricLoader.getInstance().configDir.resolve("dragnevar.json").toFile()

    private lateinit var managed: ManagedConfig<DragnevarConfigData>

    val values: DragnevarConfigData
        get() = managed.instance

    fun load() {
        if (::managed.isInitialized) return

        managed = ManagedConfig.create(configFile, DragnevarConfigData::class.java) {
            customProcessor(WaypointConnectionStatus::class.java) { option, _ ->
                WaypointConnectionStatusEditor(option)
            }
            customProcessor(WaypointConnectionButton::class.java) { option, _ ->
                WaypointConnectionButtonEditor(option)
            }
            loadFailed = BiConsumer { _, error ->
                Dragnevar.LOGGER.log(Level.WARNING, "Could not load the config.", error)
            }
            saveFailed = BiConsumer { _, error ->
                Dragnevar.LOGGER.log(Level.WARNING, "Could not save the config.", error)
            }
        }
    }

    fun openScreen() {
        managed.openConfigGui()
    }

    fun save() {
        managed.saveToFile()
    }
}
