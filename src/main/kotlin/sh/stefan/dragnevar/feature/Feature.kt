package sh.stefan.dragnevar.feature

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
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

        ClientTickEvents.END_CLIENT_TICK.register {
            tickFeatures.forEach(TickFeature::onTick)
        }

        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
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
