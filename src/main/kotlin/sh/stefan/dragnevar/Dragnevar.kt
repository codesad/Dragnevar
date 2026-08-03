package sh.stefan.dragnevar

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.Version
import sh.stefan.dragnevar.config.DragnevarConfig
import sh.stefan.dragnevar.feature.ClassDetector
import sh.stefan.dragnevar.feature.ConfigCommand
import sh.stefan.dragnevar.feature.FeatureManager
import sh.stefan.dragnevar.feature.ItemHighlighter
import sh.stefan.dragnevar.feature.TeamSyncFeature
import sh.stefan.dragnevar.feature.WaypointFeature
import sh.stefan.dragnevar.feature.dev.ItemLoreCommand
import sh.stefan.dragnevar.feature.dev.RavengardCheckCommand
import sh.stefan.dragnevar.feature.dev.TeamSyncRoomCommand
import java.util.logging.Logger

class Dragnevar : ModInitializer {
    companion object {
        const val MOD_ID = "dragnevar"
        val LOGGER = Logger.getLogger(MOD_ID)
        val VERSION: Version by lazy {
            FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .orElseThrow()
                .metadata.version
        }
    }

    override fun onInitialize() {
        DragnevarConfig.load()
        val features = buildList {
            add(ConfigCommand)
            add(ClassDetector)
            add(ItemHighlighter)
            add(TeamSyncFeature)
            add(WaypointFeature)

            if (FabricLoader.getInstance().isDevelopmentEnvironment) {
                add(ItemLoreCommand)
                add(RavengardCheckCommand)
                add(TeamSyncRoomCommand)
            }
        }

        FeatureManager.initialize(*features.toTypedArray())
    }
}
