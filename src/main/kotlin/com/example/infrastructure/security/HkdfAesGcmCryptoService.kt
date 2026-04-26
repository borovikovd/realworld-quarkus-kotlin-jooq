package com.example.infrastructure.security

import com.example.application.outport.CryptoService
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@ApplicationScoped
class HkdfAesGcmCryptoService(
    @param:ConfigProperty(name = "app.security.master-key") private val masterKeyBase64: String,
) : CryptoService {
    private val hmacEmailKey: ByteArray
    private val hmacUsernameKey: ByteArray
    private val vaultKek: ByteArray
    private val secureRandom = SecureRandom()

    init {
        val masterKey = Base64.getDecoder().decode(masterKeyBase64)
        hmacEmailKey = hkdf(masterKey, "hmac-email")
        hmacUsernameKey = hkdf(masterKey, "hmac-username")
        vaultKek = hkdf(masterKey, "vault-kek")
    }

    override fun hmacEmail(email: String): String = hmac(hmacEmailKey, email.trim().lowercase(Locale.ROOT))

    override fun hmacUsername(username: String): String = hmac(hmacUsernameKey, username.trim().lowercase(Locale.ROOT))

    override fun generateDek(): ByteArray = ByteArray(KEY_BYTES).also { secureRandom.nextBytes(it) }

    override fun encryptDek(dek: ByteArray): ByteArray = aesGcmEncrypt(vaultKek, dek)

    override fun decryptDek(keyCiphertext: ByteArray): ByteArray = aesGcmDecryptBytes(vaultKek, keyCiphertext)

    override fun encryptField(
        dek: ByteArray,
        plaintext: String,
    ): String {
        val cipherBytes = aesGcmEncrypt(dek, plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(cipherBytes)
    }

    override fun decryptField(
        dek: ByteArray,
        ciphertext: String,
    ): String {
        val cipherBytes = Base64.getDecoder().decode(ciphertext)
        return String(aesGcmDecryptBytes(dek, cipherBytes), Charsets.UTF_8)
    }

    private fun aesGcmEncrypt(
        key: ByteArray,
        plaintext: ByteArray,
    ): ByteArray {
        val nonce = ByteArray(GCM_NONCE_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, AES), GCMParameterSpec(GCM_TAG_BITS, nonce))
        val cipherAndTag = cipher.doFinal(plaintext)
        return nonce + cipherAndTag
    }

    private fun aesGcmDecryptBytes(
        key: ByteArray,
        payload: ByteArray,
    ): ByteArray {
        require(payload.size > GCM_NONCE_BYTES + GCM_TAG_BYTES) {
            "AES-GCM payload too short: ${payload.size} bytes (need > ${GCM_NONCE_BYTES + GCM_TAG_BYTES})"
        }
        val nonce = payload.copyOfRange(0, GCM_NONCE_BYTES)
        val cipherAndTag = payload.copyOfRange(GCM_NONCE_BYTES, payload.size)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, AES), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return cipher.doFinal(cipherAndTag)
    }

    private fun hmac(
        key: ByteArray,
        value: String,
    ): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(key, HMAC_SHA256))
        return mac.doFinal(value.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun hkdf(
        ikm: ByteArray,
        info: String,
        length: Int = KEY_BYTES,
    ): ByteArray {
        require(length in 1..(HKDF_MAX_BLOCKS * KEY_BYTES)) {
            "HKDF output length $length out of range [1, ${HKDF_MAX_BLOCKS * KEY_BYTES}]"
        }
        val prk = hmacRaw(ByteArray(KEY_BYTES), ikm)
        val infoBytes = info.toByteArray(Charsets.UTF_8)
        val result = ByteArray(length)
        var t = ByteArray(0)
        var pos = 0
        var counter = 1
        while (pos < length) {
            val mac = Mac.getInstance(HMAC_SHA256)
            mac.init(SecretKeySpec(prk, HMAC_SHA256))
            mac.update(t)
            mac.update(infoBytes)
            mac.update(counter.toByte())
            t = mac.doFinal()
            val n = minOf(t.size, length - pos)
            t.copyInto(result, pos, 0, n)
            pos += n
            counter++
        }
        return result
    }

    private fun hmacRaw(
        key: ByteArray,
        data: ByteArray,
    ): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(key, HMAC_SHA256))
        return mac.doFinal(data)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_BYTES = 32
        private const val HKDF_MAX_BLOCKS = 255 // RFC 5869 §2.3
        private const val GCM_NONCE_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        private const val AES = "AES"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val HMAC_SHA256 = "HmacSHA256"
    }
}
