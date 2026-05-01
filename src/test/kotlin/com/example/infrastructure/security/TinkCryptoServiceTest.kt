package com.example.infrastructure.security

import com.example.application.outport.security.CryptoService
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.mac.MacConfig
import com.google.crypto.tink.mac.PredefinedMacParameters
import io.quarkus.vault.client.VaultClient
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitEncryptParams
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient
import io.quarkus.vault.client.logging.LogConfidentialityLevel
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.net.http.HttpClient
import java.time.Duration
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

            val vault =
                GenericContainer("hashicorp/vault:1.17")
                    .withExposedPorts(8200)
                    .withEnv("VAULT_DEV_ROOT_TOKEN_ID", "test-token")
                    .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
                    .waitingFor(
                        Wait.forHttp("/v1/sys/health").forPort(8200).forStatusCode(200)
                            .withStartupTimeout(Duration.ofSeconds(30)),
                    )
            vault.start()

            val url = "http://${vault.host}:${vault.getMappedPort(8200)}"
            val client =
                VaultClient
                    .builder()
                    .baseUrl(url)
                    .clientToken("test-token")
                    .executor(JDKVaultHttpClient(HttpClient.newHttpClient()))
                    .logConfidentialityLevel(LogConfidentialityLevel.HIGH)
                    .build()

            val transit = client.secrets().transit()
            client.sys().mounts().enable("transit", "transit", null, null, null).toCompletableFuture().join()
            transit
                .createKey(
                    "app-keyset-kek",
                    io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitCreateKeyParams()
                        .setType(io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitKeyType.AES256_GCM96),
                ).toCompletableFuture()
                .join()

            val access = InsecureSecretKeyAccess.get()
            fun wrap(handle: KeysetHandle): String {
                val bytes = TinkProtoKeysetFormat.serializeKeyset(handle, access)
                return transit
                    .encrypt("app-keyset-kek", VaultSecretsTransitEncryptParams().setPlaintext(bytes))
                    .toCompletableFuture()
                    .join()
                    .ciphertext
            }

            val wrappedAead = wrap(KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM))
            val wrappedMac = wrap(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))
            val wrappedTokenMac = wrap(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))

            service = TinkCryptoService(url, "test-token", wrappedAead, wrappedMac, wrappedTokenMac)
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
