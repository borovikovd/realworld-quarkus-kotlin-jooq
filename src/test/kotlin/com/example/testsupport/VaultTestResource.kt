package com.example.testsupport

import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.mac.MacConfig
import com.google.crypto.tink.mac.PredefinedMacParameters
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.vault.client.VaultClient
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitCreateKeyParams
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitEncryptParams
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitKeyType
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient
import io.quarkus.vault.client.logging.LogConfidentialityLevel
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.net.http.HttpClient
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
        AeadConfig.register()
        MacConfig.register()

        vault.start()
        val url = "http://${vault.host}:${vault.getMappedPort(8200)}"

        val client =
            VaultClient
                .builder()
                .baseUrl(url)
                .clientToken(TOKEN)
                .executor(JDKVaultHttpClient(HttpClient.newHttpClient()))
                .logConfidentialityLevel(LogConfidentialityLevel.HIGH)
                .build()

        val transit = client.secrets().transit()
        client.sys().mounts().enable("transit", "transit", null, null, null).toCompletableFuture().join()
        transit
            .createKey(KEYSET_KEK, VaultSecretsTransitCreateKeyParams().setType(VaultSecretsTransitKeyType.AES256_GCM96))
            .toCompletableFuture()
            .join()

        val wrappedAead = wrapKeyset(transit, KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM))
        val wrappedMac = wrapKeyset(transit, KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))
        val wrappedTokenMac = wrapKeyset(transit, KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))

        return mapOf(
            "quarkus.vault.url" to url,
            "quarkus.vault.authentication.client-token" to TOKEN,
            "app.tink.aead-keyset" to wrappedAead,
            "app.tink.mac-keyset" to wrappedMac,
            "app.tink.token-mac-keyset" to wrappedTokenMac,
        )
    }

    override fun stop() {
        vault.stop()
    }

    private fun wrapKeyset(
        transit: io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransit,
        handle: KeysetHandle,
    ): String =
        transit
            .encrypt(
                KEYSET_KEK,
                VaultSecretsTransitEncryptParams()
                    .setPlaintext(TinkProtoKeysetFormat.serializeKeyset(handle, InsecureSecretKeyAccess.get())),
            ).toCompletableFuture()
            .join()
            .ciphertext

    companion object {
        private const val TOKEN = "test-root-token"
        private const val KEYSET_KEK = "app-keyset-kek"
    }
}
