package sh.stefan.dragnevar.ravengard

interface RavengardName {
    val displayName: String
}

internal fun <T : RavengardName> Iterable<T>.findByName(name: String): T? {
    val normalizedName = name.trim()
    return firstOrNull {
        it.displayName.equals(normalizedName, ignoreCase = true)
    }
}
