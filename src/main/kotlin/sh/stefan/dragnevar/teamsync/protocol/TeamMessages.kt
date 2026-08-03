package sh.stefan.dragnevar.teamsync.protocol

import com.google.gson.JsonObject
import sh.stefan.dragnevar.teamsync.TeamMember

object TeamMessages {
    const val JOINED = "joined"
    const val MEMBER_JOINED = "member_joined"
    const val MEMBER_LEFT = "member_left"

    fun members(message: JsonObject): List<TeamMember> = runCatching {
        message.getAsJsonArray("members").mapNotNull { element ->
            member(element.asJsonObject)
        }
    }.getOrDefault(emptyList())

    fun member(message: JsonObject): TeamMember? = runCatching {
        TeamMember(
            playerId = message.get("playerId").asString,
            playerName = message.get("playerName").asString
        )
    }.getOrNull()
}
