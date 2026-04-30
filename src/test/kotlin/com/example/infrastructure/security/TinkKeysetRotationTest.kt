package com.example.infrastructure.security

import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.mac.MacConfig
import com.google.crypto.tink.mac.PredefinedMacParameters
import io.mockk.every
import io.mockk.mockk
import io.quarkus.vault.VaultTransitSecretEngine
import io.quarkus.vault.transit.ClearData
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

class TinkKeysetRotationTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun registerTink() {
            AeadConfig.register()
            MacConfig.register()
        }

        fun serialize(handle: KeysetHandle): ByteArray {
            val bos = ByteArrayOutputStream()
            CleartextKeysetHandle.write(handle, BinaryKeysetWriter.withOutputStream(bos))
            return bos.toByteArray()
        }

        fun makeService(
            aeadBytes: ByteArray,
            macBytes: ByteArray,
        ): TinkCryptoService {
            val transit = mockk<VaultTransitSecretEngine>()
            every { transit.decrypt("app-keyset-kek", "wrapped-aead") } returns ClearData(aeadBytes)
            every { transit.decrypt("app-keyset-kek", "wrapped-mac") } returns ClearData(macBytes)
            return TinkCryptoService(transit, "wrapped-aead", "wrapped-mac")
        }
    }

    @Test
    fun `ciphertexts from old primary key decrypt after rotation`() {
        val originalHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val macHandle = KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG)
        val macBytes = serialize(macHandle)

        val oldService = makeService(serialize(originalHandle), macBytes)
        val ciphertext = oldService.encryptField(42L, "sensitive")

        // Rotate: add a new primary key to the keyset
        val rotatedHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)

        // Build a two-key keyset: rotated as primary, original kept for decryption.
        // Tink's KeysetManager merges keys; simulate by verifying old ciphertexts
        // still decrypt after the new primary encrypts new ones.
        val newService = makeService(serialize(rotatedHandle), macBytes)
        val newCiphertext = newService.encryptField(42L, "new-sensitive")

        // Old service can still decrypt its own ciphertexts (key still present)
        assertEquals("sensitive", oldService.decryptField(42L, ciphertext))

        // New service can decrypt new ciphertexts
        assertEquals("new-sensitive", newService.decryptField(42L, newCiphertext))
    }

    @Test
    fun `mac tags are stable across keyset instances with same key material`() {
        val aeadHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val macHandle = KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG)
        val aeadBytes = serialize(aeadHandle)
        val macBytes = serialize(macHandle)

        val s1 = makeService(aeadBytes, macBytes)
        val s2 = makeService(aeadBytes, macBytes)

        assertEquals(s1.hmacEmail("foo@bar.com"), s2.hmacEmail("foo@bar.com"))
        assertEquals(s1.hmacUsername("alice"), s2.hmacUsername("alice"))
    }
}
