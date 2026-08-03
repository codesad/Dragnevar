package sh.stefan.dragnevar.teamsync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ClientMessage

@Serializable
sealed interface ServerMessage

@Serializable
data class TeamMember(
    val playerId: String,
    val playerName: String
)

@Serializable
@SerialName("authenticate")
data class AuthenticateRequest(
    val playerId: String,
    val audience: String,
    val certificateExpiresAt: Long,
    val publicKey: String,
    val certificateSignature: String,
    val challengeSignature: String
) : ClientMessage

@Serializable
@SerialName("select_party")
data class SelectPartyRequest(
    val memberIds: List<String>
) : ClientMessage

@Serializable
@SerialName("challenge")
data class AuthChallenge(
    val challengeId: String,
    val nonce: String,
    val expiresAt: Long
) : ServerMessage

@Serializable
@SerialName("authenticated")
data class AuthenticatedMessage(
    val version: String
) : ServerMessage

@Serializable
@SerialName("joined")
data class JoinedMessage(
    val members: List<TeamMember>
) : ServerMessage

@Serializable
@SerialName("member_joined")
data class MemberJoinedMessage(
    val playerId: String,
    val playerName: String
) : ServerMessage {
    val member: TeamMember
        get() = TeamMember(playerId, playerName)
}

@Serializable
@SerialName("member_left")
data class MemberLeftMessage(
    val playerId: String
) : ServerMessage

@Serializable
@SerialName("error")
data class ErrorMessage(
    val message: String
) : ServerMessage
