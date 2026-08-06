package sh.stefan.dragnevar.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
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
    @field:Category(name = "Team Sync", desc = "Features shared with your team.")
    @JvmField
    var teamSync = TeamSyncConfig()

    override fun getTitle(): StructuredText = StructuredText.of("Dragnevar")
}

class ItemConfig {
    @field:Expose
    @field:ConfigOption(name = "Armor Highlight Color", desc = "Outline color for armor upgrades.")
    @field:ConfigEditorColour
    @JvmField
    var armorHighlightColor = ChromaColour.fromStaticRGB(0x00, 0x7B, 0xFF, 0xA0)

    @field:Expose
    @field:ConfigOption(name = "Accessory Highlight Color", desc = "Outline color for accessory upgrades.")
    @field:ConfigEditorColour
    @JvmField
    var accessoryHighlightColor = ChromaColour.fromStaticRGB(0x00, 0xE6, 0x5C, 0xA0)

    @field:Expose
    @field:ConfigOption(name = "Weapon Highlight Color", desc = "Outline color for weapon upgrades.")
    @field:ConfigEditorColour
    @JvmField
    var weaponHighlightColor = ChromaColour.fromStaticRGB(0xB0, 0x00, 0xFF, 0xA0)

    @field:Expose
    @field:ConfigOption(name = "Consumable Highlight Color", desc = "Outline color for consumables.")
    @field:ConfigEditorColour
    @JvmField
    var consumableHighlightColor = ChromaColour.fromStaticRGB(0xF5, 0x49, 0x27, 0xA0)

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

class TeamSyncConfig {
    @field:Expose
    @field:Category(name = "Connection", desc = "Team Sync server connection.")
    @JvmField
    var connection = TeamSyncConnectionConfig()

    @field:Expose
    @field:Category(name = "Waypoints", desc = "Shared waypoint pings.")
    @JvmField
    var waypoints = WaypointConfig()
}

class TeamSyncConnectionConfig {
    @field:Expose
    @field:ConfigOption(name = "Enabled", desc = "Enables shared Team Sync features.")
    @field:ConfigEditorBoolean
    @JvmField
    var enabled = true

    @field:Expose
    @field:ConfigOption(
        name = "WebSocket URL",
        desc = "Address of the Team Sync server.\nOnly change if self hosting!"
    )
    @field:ConfigEditorText
    @JvmField
    var websocketUrl = "wss://stephn.codes/dragnevar/"

    @field:ConfigOption(
        name = "Status",
        desc = "Current Team Sync server connection."
    )
    @field:TeamSyncConnectionStatus
    @Transient
    @JvmField
    var connectionStatus = Unit
}

class WaypointConfig {
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
            customProcessor(TeamSyncConnectionStatus::class.java) { option, _ ->
                TeamSyncConnectionStatusEditor(option)
            }
            loadFailed = BiConsumer { _, error ->
                Dragnevar.LOGGER.log(Level.WARNING, "Could not load the config.", error)
            }
            saveFailed = BiConsumer { _, error ->
                Dragnevar.LOGGER.log(Level.WARNING, "Could not save the config.", error)
            }
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register { save() }
    }

    fun openScreen() {
        val editor = object : MoulConfigEditor<DragnevarConfigData>(managed.processor) {
            override fun onAfterClose() {
                save()
            }
        }
        IMinecraft.INSTANCE.openWrappedScreen(editor)
    }

    fun save() {
        managed.saveToFile()
    }
}
