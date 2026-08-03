package sh.stefan.dragnevar.teamsync

import net.minecraft.client.Minecraft
import sh.stefan.dragnevar.mixin.MinecraftAccessor
import sh.stefan.dragnevar.teamsync.protocol.AuthChallenge
import sh.stefan.dragnevar.teamsync.protocol.AuthenticateRequest
import sh.stefan.dragnevar.teamsync.protocol.TeamSyncSecurity
import java.security.Signature
import java.util.Base64
import java.util.concurrent.CompletableFuture

object MinecraftIdentity {
    fun authenticate(
        challenge: AuthChallenge,
        audience: String
    ): CompletableFuture<AuthenticateRequest> {
        val minecraft = Minecraft.getInstance()
        val user = minecraft.user
        val playerId = user.profileId
        val keyPairManager = (minecraft as MinecraftAccessor).`dragnevar$getProfileKeyPairManager`()
        return keyPairManager.prepareKeyPair().thenApply { optionalPair ->
            val pair = optionalPair.orElseThrow {
                IllegalStateException("Profile key unavailable")
            }
            val certificate = pair.publicKey().data()
            val payload = TeamSyncSecurity.authenticationPayload(challenge, audience, playerId)
            val signer = Signature.getInstance("SHA256withRSA")
            signer.initSign(pair.privateKey())
            signer.update(payload)
            val proof = signer.sign()
            val base64 = Base64.getEncoder()
            AuthenticateRequest(
                playerId = playerId.toString(),
                audience = TeamSyncSecurity.normalizeAudience(audience),
                certificateExpiresAt = certificate.expiresAt().toEpochMilli(),
                publicKey = base64.encodeToString(certificate.key().encoded),
                certificateSignature = base64.encodeToString(certificate.keySignature()),
                challengeSignature = base64.encodeToString(proof)
            )
        }
    }
}
