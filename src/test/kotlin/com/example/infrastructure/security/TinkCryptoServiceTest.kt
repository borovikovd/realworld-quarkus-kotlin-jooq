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
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TinkCryptoServiceTest {
    companion object {
        private lateinit var service: TinkCryptoService

        @BeforeAll
        @JvmStatic
        fun setup() {
            AeadConfig.register()
            MacConfig.register()

            val aeadHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
            val macHandle = KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG)

            fun serialize(handle: KeysetHandle): ByteArray {
                val bos = ByteArrayOutputStream()
                CleartextKeysetHandle.write(handle, BinaryKeysetWriter.withOutputStream(bos))
                return bos.toByteArray()
            }

            val aeadBytes = serialize(aeadHandle)
            val macBytes = serialize(macHandle)

            val transit = mockk<VaultTransitSecretEngine>()
            every { transit.decrypt("app-keyset-kek", "wrapped-aead") } returns ClearData(aeadBytes)
            every { transit.decrypt("app-keyset-kek", "wrapped-mac") } returns ClearData(macBytes)

            service = TinkCryptoService(transit, "wrapped-aead", "wrapped-mac")
        }
    }

    @Test
    fun `encrypt and decrypt round-trips for same userId`() {
        val plaintext = "test@example.com"
        val ciphertext = service.encryptField(1L, plaintext)
        assertEquals(plaintext, service.decryptField(1L, ciphertext))
    }

    @Test
    fun `decrypt with wrong userId fails (AD mismatch)`() {
        val ciphertext = service.encryptField(1L, "secret")
        assertThrows<Exception> { service.decryptField(2L, ciphertext) }
    }

    @Test
    fun `hmac is deterministic`() {
        assertEquals(service.hmacEmail("foo@bar.com"), service.hmacEmail("foo@bar.com"))
        assertEquals(service.hmacUsername("alice"), service.hmacUsername("alice"))
        assertEquals(service.hmacRefreshToken("tok"), service.hmacRefreshToken("tok"))
    }

    @Test
    fun `hmacEmail normalizes whitespace and case`() {
        assertEquals(service.hmacEmail("Foo@Bar.com"), service.hmacEmail("foo@bar.com "))
        assertEquals(service.hmacUsername("Alice"), service.hmacUsername(" alice"))
    }

    @Test
    fun `different inputs produce different macs`() {
        assertNotEquals(service.hmacEmail("a@b.com"), service.hmacEmail("c@d.com"))
    }
}
