package com.example.infrastructure.security

import com.example.application.outport.CryptoService
import com.google.crypto.tink.Aead
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.Mac
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.TinkProtoKeysetFormat
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
    @param:ConfigProperty(name = "app.tink.token-mac-keyset") private val wrappedTokenMac: String,
) : CryptoService {
    private val aead: Aead
    private val mac: Mac
    private val tokenMac: Mac

    init {
        AeadConfig.register()
        MacConfig.register()
        val config = RegistryConfiguration.get()
        aead = unwrapKeyset(wrappedAead).getPrimitive(config, Aead::class.java)
        mac = unwrapKeyset(wrappedMac).getPrimitive(config, Mac::class.java)
        tokenMac = unwrapKeyset(wrappedTokenMac).getPrimitive(config, Mac::class.java)
    }

    override fun hmacEmail(email: String): String = macTag(mac, email.trim().lowercase(Locale.ROOT))

    override fun hmacUsername(username: String): String = macTag(mac, username.trim().lowercase(Locale.ROOT))

    override fun hmacRefreshToken(token: String): String = macTag(tokenMac, token)

    override fun encryptField(
        userId: Long,
        plaintext: String,
    ): ByteArray = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), userIdAd(userId))

    override fun decryptField(
        userId: Long,
        ciphertext: ByteArray,
    ): String = String(aead.decrypt(ciphertext, userIdAd(userId)), Charsets.UTF_8)

    private fun unwrapKeyset(wrapped: String) =
        TinkProtoKeysetFormat.parseKeyset(transit.decrypt(KEYSET_KEK, wrapped).value, InsecureSecretKeyAccess.get())

    private fun macTag(
        key: Mac,
        value: String,
    ): String {
        val bytes = key.computeMac(value.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun userIdAd(userId: Long): ByteArray = ByteBuffer.allocate(LONG_BYTES).putLong(userId).array()

    companion object {
        private const val KEYSET_KEK = "app-keyset-kek"
        private const val LONG_BYTES = 8
    }
}
