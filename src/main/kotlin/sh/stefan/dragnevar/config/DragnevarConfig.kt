package sh.stefan.dragnevar.config

import dev.isxander.yacl3.api.ButtonOption
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.LabelOption
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.StateManager
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import sh.stefan.dragnevar.Dragnevar
import sh.stefan.dragnevar.feature.WaypointFeature
import sh.stefan.dragnevar.utils.Chat

class DragnevarConfigData {
    @field:SerialEntry
    var weaponPriority = WeaponPriority.DPS

    @field:SerialEntry
    var websocketUrl = ""

    @field:SerialEntry
    var teamName = ""

    @field:SerialEntry
    var waypointTimeoutSeconds = 30

    @field:SerialEntry
    var playPingSound = true

    @field:SerialEntry
    var showWaypointDistance = true
}

object DragnevarConfig {
    private val handler = ConfigClassHandler.createBuilder(DragnevarConfigData::class.java)
        .id(Identifier.fromNamespaceAndPath(Dragnevar.MOD_ID, "config"))
        .serializer { config ->
            GsonConfigSerializerBuilder.create(config)
                .setPath(FabricLoader.getInstance().configDir.resolve("dragnevar.json5"))
                .setJson5(true)
                .build()
        }
        .build()

    val values: DragnevarConfigData
        get() = handler.instance()

    fun load() {
        handler.load()
    }

    fun createScreen(parent: Screen?): Screen {
        val config = values
        val defaults = handler.defaults()
        val connected = WaypointFeature.isConnected
        val weaponPriorityOption = Option.createBuilder<WeaponPriority>()
            .name(Component.translatable("dragnevar.config.weapon_priority"))
            .description(
                OptionDescription.of(
                    Component.translatable("dragnevar.config.weapon_priority.description")
                )
            )
            .binding(
                defaults.weaponPriority,
                { config.weaponPriority },
                { config.weaponPriority = it }
            )
            .controller { option ->
                EnumControllerBuilder.create(option)
                    .enumClass(WeaponPriority::class.java)
                    .formatValue { priority ->
                        Component.translatable(
                            "dragnevar.config.weapon_priority.${priority.name.lowercase()}"
                        )
                    }
            }
            .build()
        val urlOption = Option.createBuilder<String>()
            .name(Component.translatable("dragnevar.config.websocket_url"))
            .description(
                OptionDescription.of(
                    Component.translatable("dragnevar.config.websocket_url.description")
                )
            )
            .binding(defaults.websocketUrl, { config.websocketUrl }, { config.websocketUrl = it })
            .controller(StringControllerBuilder::create)
            .build()
        val teamOption = Option.createBuilder<String>()
            .name(Component.translatable("dragnevar.config.team_name"))
            .description(
                OptionDescription.of(
                    Component.translatable("dragnevar.config.team_name.description")
                )
            )
            .binding(defaults.teamName, { config.teamName }, { config.teamName = it })
            .controller(StringControllerBuilder::create)
            .build()
        val status = LabelOption.createBuilder()
            .state(
                StateManager.createSimple(
                    connectionStatus(),
                    ::connectionStatus
                ) { }
            )
            .build()
        val connectButton = ButtonOption.createBuilder()
            .name(Component.translatable("dragnevar.config.server"))
            .text(Component.translatable("dragnevar.config.connect.button"))
            .available(!connected)
            .action { _, _ ->
                config.websocketUrl = urlOption.pendingValue()
                config.teamName = teamOption.pendingValue()
                handler.save()
                WaypointFeature.connect(config.websocketUrl, config.teamName)
                    .onFailure { error ->
                        val player = Minecraft.getInstance().player
                        if (player != null) {
                            Chat.showError(player, error.message ?: "Could not connect.")
                        }
                    }
            }
            .build()
        val disconnectButton = ButtonOption.createBuilder()
            .name(Component.translatable("dragnevar.config.server"))
            .text(Component.translatable("dragnevar.config.disconnect.button"))
            .available(connected)
            .action { _, _ -> WaypointFeature.disconnect() }
            .build()
        val timeoutOption = Option.createBuilder<Int>()
            .name(Component.translatable("dragnevar.config.timeout"))
            .description(
                OptionDescription.of(
                    Component.translatable("dragnevar.config.timeout.description")
                )
            )
            .binding(
                defaults.waypointTimeoutSeconds,
                { config.waypointTimeoutSeconds },
                { config.waypointTimeoutSeconds = it }
            )
            .controller {
                IntegerSliderControllerBuilder.create(it)
                    .range(5, 120)
                    .step(5)
            }
            .build()
        val soundOption = Option.createBuilder<Boolean>()
            .name(Component.translatable("dragnevar.config.ping_sound"))
            .binding(
                defaults.playPingSound,
                { config.playPingSound },
                { config.playPingSound = it }
            )
            .controller(TickBoxControllerBuilder::create)
            .build()
        val distanceOption = Option.createBuilder<Boolean>()
            .name(Component.translatable("dragnevar.config.show_distance"))
            .binding(
                defaults.showWaypointDistance,
                { config.showWaypointDistance },
                { config.showWaypointDistance = it }
            )
            .controller(TickBoxControllerBuilder::create)
            .build()

        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("Dragnevar"))
            .category(
                ConfigCategory.createBuilder()
                    .name(Component.translatable("dragnevar.config.items"))
                    .group(
                        OptionGroup.createBuilder()
                            .name(Component.translatable("dragnevar.config.item_highlighting"))
                            .option(weaponPriorityOption)
                            .build()
                    )
                    .build()
            )
            .category(
                ConfigCategory.createBuilder()
                    .name(Component.translatable("dragnevar.config.waypoints"))
                    .group(
                        OptionGroup.createBuilder()
                            .name(Component.translatable("dragnevar.config.connection"))
                            .option(urlOption)
                            .option(teamOption)
                            .option(status)
                            .option(connectButton)
                            .option(disconnectButton)
                            .build()
                    )
                    .group(
                        OptionGroup.createBuilder()
                            .name(Component.translatable("dragnevar.config.behavior"))
                            .option(timeoutOption)
                            .option(soundOption)
                            .option(distanceOption)
                            .build()
                    )
                    .build()
            )
            .save(handler::save)
            .screenInit { screen ->
                ScreenEvents.afterTick(screen).register {
                    status.stateManager().sync()
                    val isConnected = WaypointFeature.isConnected
                    connectButton.setAvailable(!isConnected)
                    disconnectButton.setAvailable(isConnected)
                }
            }
            .build()
            .generateScreen(parent)
    }

    private fun connectionStatus(): Component = Component.translatable(
        if (WaypointFeature.isConnected) {
            "dragnevar.config.status.connected"
        } else {
            "dragnevar.config.status.disconnected"
        }
    )
}
