package com.example.infrastructure.security

import com.example.application.outport.CryptoService
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.mac.MacConfig
import com.google.crypto.tink.mac.PredefinedMacParameters
import io.mockk.every
import io.mockk.mockk
import io.quarkus.vault.client.VaultClient
import io.quarkus.vault.client.api.VaultSecretsAccessor
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransit
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitDecryptParams
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
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

            val access = InsecureSecretKeyAccess.get()
            fun serialize(handle: KeysetHandle) = TinkProtoKeysetFormat.serializeKeyset(handle, access)

            val aeadBytes = serialize(KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM))
            val macBytes = serialize(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))
            val tokenMacBytes = serialize(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))

            service = TinkCryptoService(
                mockVaultClient("app-keyset-kek", "wrapped-aead" to aeadBytes, "wrapped-mac" to macBytes, "wrapped-token-mac" to tokenMacBytes),
                "wrapped-aead",
                "wrapped-mac",
                "wrapped-token-mac",
            )
        }

        fun mockVaultClient(
            keyName: String,
            vararg ciphertextToPlaintext: Pair<String, ByteArray>,
        ): VaultClient {
            val transit = mockk<VaultSecretsTransit>()
            for ((ciphertext, plaintext) in ciphertextToPlaintext) {
                every {
                    transit.decrypt(keyName, match<VaultSecretsTransitDecryptParams> { it.ciphertext == ciphertext })
                } returns CompletableFuture.completedFuture(plaintext)
            }
            val secrets = mockk<VaultSecretsAccessor>()
            every { secrets.transit() } returns transit
            val client = mockk<VaultClient>()
            every { client.secrets() } returns secrets
            return client
        }
    }

    @Test
    fun `encrypt and decrypt round-trips for same userId and field`() {
        val plaintext = "test@example.com"
        val ciphertext = service.encryptField(1L, CryptoService.EMAIL, plaintext)
        assertEquals(plaintext, service.decryptField(1L, CryptoService.EMAIL, ciphertext))
    }

    @Test
    fun `decrypt with wrong userId fails (AD mismatch)`() {
        val ciphertext = service.encryptField(1L, CryptoService.EMAIL, "secret")
        assertThrows<Exception> { service.decryptField(2L, CryptoService.EMAIL, ciphertext) }
    }

    @Test
    fun `decrypt with wrong field fails (AD mismatch)`() {
        val ciphertext = service.encryptField(1L, CryptoService.EMAIL, "secret")
        assertThrows<Exception> { service.decryptField(1L, CryptoService.USERNAME, ciphertext) }
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

    @Test
    fun `hmacRefreshToken uses separate keyset from hmacEmail`() {
        assertNotEquals(service.hmacRefreshToken("alice"), service.hmacEmail("alice"))
    }
}
