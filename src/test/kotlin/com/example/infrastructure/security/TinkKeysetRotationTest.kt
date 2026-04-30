package com.example.infrastructure.security

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetManager
import com.google.crypto.tink.Mac
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.mac.MacConfig
import com.google.crypto.tink.mac.PredefinedMacParameters
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests Tink's multi-key keyset rotation contract directly.
 * TinkCryptoService wraps these primitives; rotation behavior is Tink's responsibility.
 */
class TinkKeysetRotationTest {
    companion object {
        private lateinit var config: com.google.crypto.tink.Configuration

        @BeforeAll
        @JvmStatic
        fun registerTink() {
            AeadConfig.register()
            MacConfig.register()
            config = RegistryConfiguration.get()
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
    }

    @Test
    fun `ciphertexts encrypted with old primary key decrypt after keyset rotation`() {
        val keysetWithA = com.google.crypto.tink.KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val aeadA = keysetWithA.getPrimitive(config, Aead::class.java)

        val ad = fieldAd(42L, "email")
        val ciphertext = aeadA.encrypt("sensitive".toByteArray(), ad)

        // Rotate: add key B and promote it to primary; key A is retained for decryption
        val manager = KeysetManager.withKeysetHandle(keysetWithA)
        val newKeyId = manager.addNewKey(AeadKeyTemplates.AES256_GCM, false)
        manager.setPrimary(newKeyId)
        val rotatedKeyset = manager.getKeysetHandle()
        val aeadAB = rotatedKeyset.getPrimitive(config, Aead::class.java)

        // Pre-rotation ciphertext (written with key A) still decrypts with the rotated keyset
        assertEquals("sensitive", String(aeadAB.decrypt(ciphertext, ad)))

        // New writes use key B (the new primary)
        val newCiphertext = aeadAB.encrypt("new-sensitive".toByteArray(), ad)
        assertEquals("new-sensitive", String(aeadAB.decrypt(newCiphertext, ad)))

        // Old AEAD with only key A cannot decrypt new ciphertext (different key)
        assertNotEquals(ciphertext.toList(), newCiphertext.toList())
    }

    @Test
    fun `mac tags are stable across keyset instances with same key material`() {
        val macHandle = com.google.crypto.tink.KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG)
        val mac1 = macHandle.getPrimitive(config, Mac::class.java)
        val mac2 = macHandle.getPrimitive(config, Mac::class.java)

        val input = "foo@bar.com".toByteArray()
        assertEquals(
            Base64.getEncoder().encodeToString(mac1.computeMac(input)),
            Base64.getEncoder().encodeToString(mac2.computeMac(input)),
        )
    }
}
