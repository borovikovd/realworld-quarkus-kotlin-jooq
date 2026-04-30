package com.example.infrastructure.security

import com.example.application.outport.CryptoService
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.KeysetManager
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.mac.MacConfig
import com.google.crypto.tink.mac.PredefinedMacParameters
import io.mockk.every
import io.mockk.mockk
import io.quarkus.vault.VaultTransitSecretEngine
import io.quarkus.vault.transit.ClearData
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TinkKeysetRotationTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun registerTink() {
            AeadConfig.register()
            MacConfig.register()
        }

        fun serialize(handle: KeysetHandle): ByteArray =
            TinkProtoKeysetFormat.serializeKeyset(handle, InsecureSecretKeyAccess.get())

        fun makeService(
            aeadBytes: ByteArray,
            macBytes: ByteArray,
            tokenMacBytes: ByteArray,
        ): TinkCryptoService {
            val transit = mockk<VaultTransitSecretEngine>()
            every { transit.decrypt("app-keyset-kek", "wrapped-aead") } returns ClearData(aeadBytes)
            every { transit.decrypt("app-keyset-kek", "wrapped-mac") } returns ClearData(macBytes)
            every { transit.decrypt("app-keyset-kek", "wrapped-token-mac") } returns ClearData(tokenMacBytes)
            return TinkCryptoService(transit, "wrapped-aead", "wrapped-mac", "wrapped-token-mac")
        }
    }

    @Test
    fun `ciphertexts encrypted with old primary key decrypt after keyset rotation`() {
        val macBytes = serialize(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))
        val tokenMacBytes = serialize(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))

        // Build a two-key AEAD keyset: key A is primary
        val keysetWithA = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val serviceBeforeRotation = makeService(serialize(keysetWithA), macBytes, tokenMacBytes)
        val ciphertext = serviceBeforeRotation.encryptField(42L, CryptoService.EMAIL, "sensitive")

        // Rotate: add key B and promote it to primary; key A is retained for decryption
        val manager = KeysetManager.withKeysetHandle(keysetWithA)
        val newKeyId = manager.addNewKey(AeadKeyTemplates.AES256_GCM, false)
        manager.setPrimary(newKeyId)
        val rotatedKeyset = manager.getKeysetHandle()

        val serviceAfterRotation = makeService(serialize(rotatedKeyset), macBytes, tokenMacBytes)

        // Pre-rotation ciphertext (written with key A) still decrypts with the rotated keyset
        assertEquals("sensitive", serviceAfterRotation.decryptField(42L, CryptoService.EMAIL, ciphertext))

        // New writes use key B (the new primary); old service with key A only cannot decrypt them
        val newCiphertext = serviceAfterRotation.encryptField(42L, CryptoService.EMAIL, "new-sensitive")
        assertEquals("new-sensitive", serviceAfterRotation.decryptField(42L, CryptoService.EMAIL, newCiphertext))
    }

    @Test
    fun `mac tags are stable across keyset instances with same key material`() {
        val aeadBytes = serialize(KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM))
        val macBytes = serialize(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))
        val tokenMacBytes = serialize(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))

        val s1 = makeService(aeadBytes, macBytes, tokenMacBytes)
        val s2 = makeService(aeadBytes, macBytes, tokenMacBytes)

        assertEquals(s1.hmacEmail("foo@bar.com"), s2.hmacEmail("foo@bar.com"))
        assertEquals(s1.hmacUsername("alice"), s2.hmacUsername("alice"))
        assertEquals(s1.hmacRefreshToken("tok"), s2.hmacRefreshToken("tok"))
    }
}
