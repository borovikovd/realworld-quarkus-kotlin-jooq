package com.example.testsupport

import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.mac.MacConfig
import com.google.crypto.tink.mac.PredefinedMacParameters
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

class VaultTestResource : QuarkusTestResourceLifecycleManager {
    private val vault =
        GenericContainer("hashicorp/vault:1.17")
            .withExposedPorts(8200)
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", TOKEN)
            .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
            .waitingFor(
                Wait.forHttp("/v1/sys/health").forPort(8200).forStatusCode(200)
                    .withStartupTimeout(Duration.ofSeconds(30)),
            )

    override fun start(): Map<String, String> {
        AeadConfig.register()
        MacConfig.register()

        vault.start()
        val url = "http://${vault.host}:${vault.getMappedPort(8200)}"
        val http = HttpClient.newHttpClient()
        http.post(url, "/v1/sys/mounts/transit", """{"type":"transit"}""")
        http.post(url, "/v1/transit/keys/$KEYSET_KEK", """{"type":"aes256-gcm96"}""")

        val wrappedAead = wrapKeyset(http, url, KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM))
        val wrappedMac = wrapKeyset(http, url, KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))

        return mapOf(
            "quarkus.vault.url" to url,
            "quarkus.vault.authentication.client-token" to TOKEN,
            "app.tink.aead-keyset" to wrappedAead,
            "app.tink.mac-keyset" to wrappedMac,
        )
    }

    override fun stop() {
        vault.stop()
    }

    private fun wrapKeyset(
        http: HttpClient,
        baseUrl: String,
        handle: KeysetHandle,
    ): String {
        val bos = ByteArrayOutputStream()
        CleartextKeysetHandle.write(handle, BinaryKeysetWriter.withOutputStream(bos))
        val plaintext = Base64.getEncoder().encodeToString(bos.toByteArray())
        val body = """{"plaintext":"$plaintext"}"""
        val response =
            http.send(
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("$baseUrl/v1/transit/encrypt/$KEYSET_KEK"))
                    .header("X-Vault-Token", TOKEN)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )
        val json = response.body()
        val start = json.indexOf("\"ciphertext\":\"") + 14
        val end = json.indexOf('"', start)
        return json.substring(start, end)
    }

    private fun HttpClient.post(
        baseUrl: String,
        path: String,
        body: String,
    ) {
        send(
            HttpRequest
                .newBuilder()
                .uri(URI.create("$baseUrl$path"))
                .header("X-Vault-Token", TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.discarding(),
        )
    }

    companion object {
        private const val TOKEN = "test-root-token"
        private const val KEYSET_KEK = "app-keyset-kek"
    }
}
