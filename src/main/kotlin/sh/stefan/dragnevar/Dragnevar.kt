package sh.stefan.dragnevar

import net.fabricmc.api.ModInitializer
import sh.stefan.dragnevar.feature.ClassDetector
import sh.stefan.dragnevar.feature.FeatureManager
import sh.stefan.dragnevar.feature.ItemHighlighter
import java.util.logging.Logger

class Dragnevar : ModInitializer {
    companion object {
        const val MOD_ID = "dragnevar"
        val LOGGER = Logger.getLogger(MOD_ID)
    }

    override fun onInitialize() {
        FeatureManager.initialize(ClassDetector, ItemHighlighter)
    }
}
