package sh.stefan.dragnevar.feature

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.ItemStack
import sh.stefan.dragnevar.ravengard.Profile
import sh.stefan.dragnevar.ravengard.RavengardDetector
import sh.stefan.dragnevar.ravengard.findByName
import sh.stefan.dragnevar.utils.Chat

object ClassDetector : Feature(), ContainerOpenFeature, WorldConnectionFeature {
    private const val PROFILE_MENU_SLOT_INDEX = 27

    var currentProfile: Profile? = null
        private set

    internal fun setProfile(profile: Profile) {
        currentProfile = profile
    }

    override fun onWorldJoin() {
        if (RavengardDetector.isOnRavengard() && currentProfile == null) {
            Chat.sendPrefixMessage(
                "&cMake sure to open the Ravengard Main Menu so the mod can detect your class!"
            )
        }
    }

    override fun onWorldLeave() {
        currentProfile = null
    }

    override fun onContainerOpen(screen: AbstractContainerScreen<*>) {
        val title = Chat.stripFormatting(screen.title.string)
        if (!title.contains("Main Menu", ignoreCase = true)) return

        val detectedProfile = screen.menu.slots
            .getOrNull(PROFILE_MENU_SLOT_INDEX)
            ?.item
            ?.takeUnless { it.isEmpty }
            ?.let(::parseProfile)

        if (detectedProfile != null && detectedProfile != currentProfile) {
            Chat.sendPrefixMessage(
                "&aDetected your class: &b${detectedProfile.displayName}&a!"
            )
        }
        currentProfile = detectedProfile
    }

    private fun parseProfile(item: ItemStack): Profile? {
        val name = Chat.stripFormatting(item.hoverName.string)
            .substringAfterLast(" - ", missingDelimiterValue = "")
        return Profile.entries.findByName(name)
    }
}
