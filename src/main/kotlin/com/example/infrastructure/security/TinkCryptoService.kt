package com.example.infrastructure.security

import com.example.application.outport.security.CryptoService
import com.google.crypto.tink.Aead
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.Mac
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.mac.MacConfig
import io.quarkus.vault.client.VaultClient
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitDecryptParams
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient
import io.quarkus.vault.client.logging.LogConfidentialityLevel
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.http.HttpClient
import java.nio.ByteBuffer
import java.util.Base64
import java.util.Locale

@ApplicationScoped
class TinkCryptoService(
    @param:ConfigProperty(name = "quarkus.vault.url") private val vaultUrl: String,
    @param:ConfigProperty(name = "quarkus.vault.authentication.client-token") private val vaultToken: String,
    @param:ConfigProperty(name = "app.tink.aead.keyset") private val wrappedAead: String,
    @param:ConfigProperty(name = "app.tink.mac.keyset") private val wrappedMac: String,
    @param:ConfigProperty(name = "app.tink.token.mac.keyset") private val wrappedTokenMac: String,
) : CryptoService {
    private val aead: Aead
    private val mac: Mac
    private val tokenMac: Mac

    init {
        AeadConfig.register()
        MacConfig.register()
        val config = RegistryConfiguration.get()
        val transit =
            VaultClient
                .builder()
                .baseUrl(vaultUrl)
                .clientToken(vaultToken)
                .executor(JDKVaultHttpClient(HttpClient.newHttpClient()))
                .logConfidentialityLevel(LogConfidentialityLevel.HIGH)
                .build()
                .secrets()
                .transit()
        aead = unwrapKeyset(transit, wrappedAead).getPrimitive(config, Aead::class.java)
        mac = unwrapKeyset(transit, wrappedMac).getPrimitive(config, Mac::class.java)
        tokenMac = unwrapKeyset(transit, wrappedTokenMac).getPrimitive(config, Mac::class.java)
    }

    override fun hmacEmail(email: String): String = macTag(mac, email.trim().lowercase(Locale.ROOT))

    override fun hmacUsername(username: String): String = macTag(mac, username.trim().lowercase(Locale.ROOT))

    override fun hmacRefreshToken(token: String): String = macTag(tokenMac, token)

    override fun encryptField(
        userId: Long,
        field: String,
        plaintext: String,
    ): ByteArray = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), fieldAd(userId, field))

    override fun decryptField(
        userId: Long,
        field: String,
        ciphertext: ByteArray,
    ): String = String(aead.decrypt(ciphertext, fieldAd(userId, field)), Charsets.UTF_8)

    private fun unwrapKeyset(
        transit: io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransit,
        wrapped: String,
    ): com.google.crypto.tink.KeysetHandle {
        val raw =
            transit
                .decrypt(KEYSET_KEK, VaultSecretsTransitDecryptParams().setCiphertext(wrapped))
                .toCompletableFuture()
                .join()
        return TinkProtoKeysetFormat.parseKeyset(raw, InsecureSecretKeyAccess.get())
    }

    private fun macTag(
        key: Mac,
        value: String,
    ): String {
        val bytes = key.computeMac(value.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun fieldAd(
        userId: Long,
        field: String,
    ): ByteArray {
        val fieldBytes = field.toByteArray(Charsets.UTF_8)
        return ByteBuffer
            .allocate(Long.SIZE_BYTES + Int.SIZE_BYTES + fieldBytes.size)
            .putLong(userId)
            .putInt(fieldBytes.size)
            .put(fieldBytes)
            .array()
    }

    companion object {
        private const val KEYSET_KEK = "app-keyset-kek"
    }
}
