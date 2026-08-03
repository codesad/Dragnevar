package sh.stefan.dragnevar.feature

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import sh.stefan.dragnevar.Dragnevar
import java.util.Collections
import java.util.WeakHashMap

abstract class Feature {
    protected val logger = Dragnevar.LOGGER

    protected val player
        get() = Minecraft.getInstance().player
}

interface TickFeature {
    fun onTick()
}

interface ContainerOpenFeature {
    fun onContainerOpen(screen: AbstractContainerScreen<*>)
}

interface CommandFeature {
    fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>)
}

interface KeybindFeature {
    val keyMapping: KeyMapping
    fun onKeybind()
}

interface HudRenderFeature {
    val hudElementId: Identifier
    fun renderHud(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker)
}

interface WorldConnectionFeature {
    fun onWorldJoin() {}

    fun onWorldLeave() {}
}

interface GameMessageFeature {
    fun onGameMessage(message: Component, overlay: Boolean)
}

object FeatureManager {
    // screens can get initialized again, so don't register another callback for the same one
    private val initializedContainerScreens = Collections.newSetFromMap(
        WeakHashMap<AbstractContainerScreen<*>, Boolean>()
    )
    private var initialized = false

    fun initialize(vararg features: Feature) {
        check(!initialized) { "Features have already been initialized" }
        initialized = true

        val tickFeatures = features.filterIsInstance<TickFeature>()
        val containerOpenFeatures = features.filterIsInstance<ContainerOpenFeature>()
        val commandFeatures = features.filterIsInstance<CommandFeature>()
        val keybindFeatures = features.filterIsInstance<KeybindFeature>()
        val hudRenderFeatures = features.filterIsInstance<HudRenderFeature>()
        val worldConnectionFeatures = features.filterIsInstance<WorldConnectionFeature>()
        val gameMessageFeatures = features.filterIsInstance<GameMessageFeature>()

        keybindFeatures.forEach {
            KeyMappingHelper.registerKeyMapping(it.keyMapping)
        }

        hudRenderFeatures.forEach { feature ->
            HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                feature.hudElementId,
                feature::renderHud
            )
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            tickFeatures.forEach(TickFeature::onTick)
            keybindFeatures.forEach { feature ->
                while (feature.keyMapping.consumeClick()) feature.onKeybind()
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            commandFeatures.forEach { it.registerCommands(dispatcher) }
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            worldConnectionFeatures.forEach(WorldConnectionFeature::onWorldJoin)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            worldConnectionFeatures.forEach(WorldConnectionFeature::onWorldLeave)
        }

        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            gameMessageFeatures.forEach { it.onGameMessage(message, overlay) }
        }

        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            ScreenMouseEvents.allowMouseClick(screen).register { _, event ->
                val matchingFeatures = keybindFeatures.filter {
                    it.keyMapping.matchesMouse(event)
                }
                matchingFeatures.forEach(KeybindFeature::onKeybind)
                matchingFeatures.isEmpty()
            }

            ScreenKeyboardEvents.allowKeyPress(screen).register { _, event ->
                val matchingFeatures = keybindFeatures.filter {
                    it.keyMapping.matches(event)
                }
                matchingFeatures.forEach(KeybindFeature::onKeybind)
                matchingFeatures.isEmpty()
            }

            if (screen is AbstractContainerScreen<*> && initializedContainerScreens.add(screen)) {
                var opened = false
                ScreenEvents.afterTick(screen).register {
                    // wait one screen tick so the menu has time to populate its items
                    if (!opened) {
                        opened = true
                        containerOpenFeatures.forEach { it.onContainerOpen(screen) }
                    }
                }
            }
        }
    }
}
