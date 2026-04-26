package com.example.infrastructure.security

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@ApplicationScoped
class CryptoService(
    @param:ConfigProperty(name = "app.security.master-key") private val masterKeyBase64: String,
) {
    private val hmacEmailKey: ByteArray
    private val hmacUsernameKey: ByteArray

    init {
        val masterKey = Base64.getDecoder().decode(masterKeyBase64)
        hmacEmailKey = hkdf(masterKey, "hmac-email")
        hmacUsernameKey = hkdf(masterKey, "hmac-username")
    }

    fun hmacEmail(email: String): String = hmac(hmacEmailKey, email.lowercase())

    fun hmacUsername(username: String): String = hmac(hmacUsernameKey, username.lowercase())

    // Phase 1: identity functions — wire AES-256-GCM in Phase 4
    fun encryptField(plaintext: String): String = plaintext

    fun decryptField(ciphertext: String): String = ciphertext

    private fun hmac(
        key: ByteArray,
        value: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(value.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun hkdf(
        ikm: ByteArray,
        info: String,
    ): ByteArray {
        val zeroes = ByteArray(KEY_BYTES)
        val prk = hmacRaw(zeroes, ikm)
        val infoBytes = info.toByteArray(Charsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(infoBytes)
        mac.update(0x01.toByte())
        return mac.doFinal()
    }

    private fun hmacRaw(
        key: ByteArray,
        data: ByteArray,
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_BYTES = 32
    }
}
