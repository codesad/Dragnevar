package sh.stefan.dragnevar.ravengard.label

import sh.stefan.dragnevar.ravengard.Profile

enum class ProfileLabel(
    override val character: Char,
    val profile: Profile
) : RavengardLabel {
    KNIGHT('\uE210', Profile.KNIGHT),
    WARRIOR('\uE21E', Profile.WARRIOR),
    HUNTER('\uE221', Profile.HUNTER)
}
