package sh.stefan.dragnevar.ravengard

interface RavengardLabel {
    val character: Char
}

internal fun <T : RavengardLabel> Iterable<T>.findIn(text: Iterable<String>): T? {
    // the resource pack uses private-use characters as its item labels
    return firstOrNull { label -> text.any { label.character in it } }
}

internal fun <T : RavengardLabel> Iterable<T>.findAllIn(text: Iterable<String>): List<T> {
    return filter { label -> text.any { label.character in it } }
}
