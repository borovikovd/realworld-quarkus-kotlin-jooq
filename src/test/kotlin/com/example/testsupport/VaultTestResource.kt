package com.example.testsupport

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

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
        vault.start()
        val url = "http://${vault.host}:${vault.getMappedPort(8200)}"
        configureVault(url)
        return mapOf(
            "quarkus.vault.url" to url,
            "quarkus.vault.authentication.client-token" to TOKEN,
        )
    }

    override fun stop() {
        vault.stop()
    }

    private fun configureVault(url: String) {
        val http = HttpClient.newHttpClient()
        http.post(url, "/v1/sys/mounts/transit", """{"type":"transit"}""")
        for (key in listOf("hmac-email", "hmac-username", "hmac-refresh-token", "user-data-kek")) {
            http.post(url, "/v1/transit/keys/$key", """{"type":"aes256-gcm96"}""")
        }
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
    }
}
