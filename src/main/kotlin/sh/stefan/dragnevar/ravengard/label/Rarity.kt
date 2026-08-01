package sh.stefan.dragnevar.ravengard.label

enum class Rarity(override val character: Char) : RavengardLabel {
    COMMON('\uE203'),
    UNCOMMON('\uE21C'),
    RARE('\uE218'),
    EPIC('\uE208'),
    LEGENDARY('\uE211')
}
