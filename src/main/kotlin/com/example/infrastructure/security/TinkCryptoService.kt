package com.example.infrastructure.security

import com.example.application.outport.CryptoService
import com.google.crypto.tink.Aead
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.Mac
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.mac.MacConfig
import io.quarkus.vault.VaultTransitSecretEngine
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.nio.ByteBuffer
import java.util.Base64
import java.util.Locale

@ApplicationScoped
class TinkCryptoService(
    private val transit: VaultTransitSecretEngine,
    @param:ConfigProperty(name = "app.tink.aead-keyset") private val wrappedAead: String,
    @param:ConfigProperty(name = "app.tink.mac-keyset") private val wrappedMac: String,
) : CryptoService {
    private val aead: Aead
    private val mac: Mac

    init {
        AeadConfig.register()
        MacConfig.register()
        aead = unwrapKeyset(wrappedAead).getPrimitive(Aead::class.java)
        mac = unwrapKeyset(wrappedMac).getPrimitive(Mac::class.java)
    }

    override fun hmacEmail(email: String): String = macTag(email.trim().lowercase(Locale.ROOT))

    override fun hmacUsername(username: String): String = macTag(username.trim().lowercase(Locale.ROOT))

    override fun hmacRefreshToken(token: String): String = macTag(token)

    override fun encryptField(
        userId: Long,
        plaintext: String,
    ): ByteArray = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), userIdAd(userId))

    override fun decryptField(
        userId: Long,
        ciphertext: ByteArray,
    ): String = String(aead.decrypt(ciphertext, userIdAd(userId)), Charsets.UTF_8)

    private fun unwrapKeyset(wrapped: String): KeysetHandle {
        val raw = transit.decrypt(KEYSET_KEK, wrapped).value
        return CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(raw))
    }

    private fun macTag(value: String): String {
        val bytes = mac.computeMac(value.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun userIdAd(userId: Long): ByteArray = ByteBuffer.allocate(LONG_BYTES).putLong(userId).array()

    companion object {
        private const val KEYSET_KEK = "app-keyset-kek"
        private const val LONG_BYTES = 8
    }
}
