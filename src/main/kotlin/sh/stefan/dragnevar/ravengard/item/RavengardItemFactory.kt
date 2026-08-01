package sh.stefan.dragnevar.ravengard.item

internal interface RavengardItemParser<out T : RavengardItem> {
    fun from(data: RavengardItemData): T?
}

internal object RavengardItemFactory {
    // the first parser that recognizes the item gets to create it
    private val parsers: List<RavengardItemParser<RavengardItem>> = listOf(
        RavengardWeapon,
        RavengardArmor,
        RavengardAccessory
    )

    fun create(data: RavengardItemData): RavengardItem? =
        parsers.firstNotNullOfOrNull { it.from(data) }
}
