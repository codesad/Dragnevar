package sh.stefan.dragnevar

import net.fabricmc.api.ModInitializer
import sh.stefan.dragnevar.config.DragnevarConfig
import sh.stefan.dragnevar.feature.ClassDetector
import sh.stefan.dragnevar.feature.ConfigCommand
import sh.stefan.dragnevar.feature.FeatureManager
import sh.stefan.dragnevar.feature.ItemHighlighter
import sh.stefan.dragnevar.feature.ItemLoreCommand
import sh.stefan.dragnevar.feature.RavengardCheckCommand
import sh.stefan.dragnevar.feature.WaypointFeature
import java.util.logging.Logger

class Dragnevar : ModInitializer {
    companion object {
        const val MOD_ID = "dragnevar"
        val LOGGER = Logger.getLogger(MOD_ID)
    }

    override fun onInitialize() {
        DragnevarConfig.load()
        FeatureManager.initialize(
            ConfigCommand,
            ClassDetector,
            ItemHighlighter,
            ItemLoreCommand,
            RavengardCheckCommand,
            WaypointFeature
        )
    }
}
