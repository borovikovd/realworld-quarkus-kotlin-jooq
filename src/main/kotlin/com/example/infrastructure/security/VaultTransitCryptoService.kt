package com.example.infrastructure.security

import com.example.application.outport.CryptoService
import io.quarkus.vault.VaultTransitSecretEngine
import io.quarkus.vault.client.VaultClient
import io.quarkus.vault.client.api.common.VaultHashAlgorithm
import io.quarkus.vault.transit.ClearData
import jakarta.enterprise.context.ApplicationScoped
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@ApplicationScoped
class VaultTransitCryptoService(
    private val transit: VaultTransitSecretEngine,
    private val vaultClient: VaultClient,
) : CryptoService {
    private val secureRandom = SecureRandom()

    override fun hmacEmail(email: String): String = normalizedHmac(HMAC_EMAIL_KEY, email)

    override fun hmacUsername(username: String): String = normalizedHmac(HMAC_USERNAME_KEY, username)

    override fun hmacRefreshToken(token: String): String = vaultHmac(HMAC_REFRESH_TOKEN_KEY, token)

    override fun generateDek(): ByteArray = ByteArray(KEY_BYTES).also { secureRandom.nextBytes(it) }

    override fun encryptDek(dek: ByteArray): String = transit.encrypt(USER_DATA_KEK, ClearData(dek), null)

    override fun decryptDek(keyCiphertext: String): ByteArray = transit.decrypt(USER_DATA_KEK, keyCiphertext).getValue()

    override fun encryptField(
        dek: ByteArray,
        plaintext: String,
    ): ByteArray = aesGcmEncrypt(dek, plaintext.toByteArray(Charsets.UTF_8))

    override fun decryptField(
        dek: ByteArray,
        ciphertext: ByteArray,
    ): String = String(aesGcmDecrypt(dek, ciphertext), Charsets.UTF_8)

    private fun normalizedHmac(
        keyName: String,
        value: String,
    ): String = vaultHmac(keyName, value.trim().lowercase(Locale.ROOT))

    private fun vaultHmac(
        keyName: String,
        value: String,
    ): String =
        vaultClient
            .secrets()
            .transit()
            .hmac(keyName, VaultHashAlgorithm.SHA2_256, value.toByteArray(Charsets.UTF_8), null)
            .toCompletableFuture()
            .join()

    private fun aesGcmEncrypt(
        key: ByteArray,
        plaintext: ByteArray,
    ): ByteArray {
        val nonce = ByteArray(GCM_NONCE_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, AES), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return nonce + cipher.doFinal(plaintext)
    }

    private fun aesGcmDecrypt(
        key: ByteArray,
        payload: ByteArray,
    ): ByteArray {
        require(payload.size >= GCM_NONCE_BYTES + GCM_TAG_BYTES) {
            "AES-GCM payload too short: ${payload.size} bytes"
        }
        val nonce = payload.copyOfRange(0, GCM_NONCE_BYTES)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, AES), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return cipher.doFinal(payload.copyOfRange(GCM_NONCE_BYTES, payload.size))
    }

    companion object {
        private const val HMAC_EMAIL_KEY = "hmac-email"
        private const val HMAC_USERNAME_KEY = "hmac-username"
        private const val HMAC_REFRESH_TOKEN_KEY = "hmac-refresh-token"
        private const val USER_DATA_KEK = "user-data-kek"
        private const val KEY_BYTES = 32
        private const val GCM_NONCE_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        private const val AES = "AES"
        private const val AES_GCM = "AES/GCM/NoPadding"
    }
}
