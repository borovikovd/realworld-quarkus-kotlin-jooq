package com.example.tools

import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkJsonProtoKeysetFormat
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.mac.MacConfig
import com.google.crypto.tink.mac.PredefinedMacParameters
import io.quarkus.vault.client.VaultClient
import io.quarkus.vault.client.VaultClientException
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransit
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitCreateKeyParams
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitEncryptParams
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitKeyType
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient
import io.quarkus.vault.client.logging.LogConfidentialityLevel
import java.io.File
import java.net.http.HttpClient
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

private const val KEYSET_KEK = "app-keyset-kek"

fun main(args: Array<String>) {
    val vaultAddr = args.getOrElse(0) { "http://localhost:8200" }
    val vaultToken = args.getOrElse(1) { error("VAULT_TOKEN is required") }

    AeadConfig.register()
    MacConfig.register()

    val client =
        VaultClient
            .builder()
            .baseUrl(vaultAddr)
            .clientToken(vaultToken)
            .executor(JDKVaultHttpClient(HttpClient.newHttpClient()))
            .logConfidentialityLevel(LogConfidentialityLevel.HIGH)
            .build()

    val transit = client.secrets().transit()

    ensureTransitMounted(client)
    ensureKekExists(transit)

    val aeadHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
    val macHandle = KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG)
    val tokenMacHandle = KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG)

    val wrappedAead = wrap(transit, aeadHandle)
    val wrappedMac = wrap(transit, macHandle)
    val wrappedTokenMac = wrap(transit, tokenMacHandle)

    println("# ── Runtime env vars ────────────────────────────────────────────────────────")
    println("# Set these in your deployment (k8s Secret, Vault KV, etc.).")
    println("APP_TINK_AEAD_KEYSET=$wrappedAead")
    println("APP_TINK_MAC_KEYSET=$wrappedMac")
    println("APP_TINK_TOKEN_MAC_KEYSET=$wrappedTokenMac")

    writeBackup(aeadHandle, macHandle, tokenMacHandle)
}

private fun writeBackup(
    aeadHandle: KeysetHandle,
    macHandle: KeysetHandle,
    tokenMacHandle: KeysetHandle,
) {
    val aeadFile = File("keyset-aead-backup.json")
    val macFile = File("keyset-mac-backup.json")
    val tokenMacFile = File("keyset-token-mac-backup.json")

    val access = InsecureSecretKeyAccess.get()
    aeadFile.writeText(TinkJsonProtoKeysetFormat.serializeKeyset(aeadHandle, access))
    macFile.writeText(TinkJsonProtoKeysetFormat.serializeKeyset(macHandle, access))
    tokenMacFile.writeText(TinkJsonProtoKeysetFormat.serializeKeyset(tokenMacHandle, access))

    for (file in listOf(aeadFile, macFile, tokenMacFile)) {
        try {
            Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"))
        } catch (_: UnsupportedOperationException) {
            System.err.println("Warning: POSIX permissions not supported — secure ${file.absolutePath} manually")
        }
    }

    println()
    println("# ── Cold backup written (mode 600) ─────────────────────────────────────────")
    println("# ${aeadFile.absolutePath}")
    println("# ${macFile.absolutePath}")
    println("# ${tokenMacFile.absolutePath}")
    println("# JSON format — readable by any Tink implementation.")
    println("# Store offline. Delete after securing.")
}

private fun ensureTransitMounted(client: VaultClient) {
    try {
        client.sys().mounts().enable("transit", "transit", null, null, null).toCompletableFuture().join()
    } catch (e: VaultClientException) {
        if (e.status != 400) throw e
        // transit already mounted — that's fine
    }
}

private fun ensureKekExists(transit: VaultSecretsTransit) {
    try {
        transit.readKey(KEYSET_KEK).toCompletableFuture().join()
    } catch (e: VaultClientException) {
        if (e.status != 404) throw e
        transit
            .createKey(KEYSET_KEK, VaultSecretsTransitCreateKeyParams().setType(VaultSecretsTransitKeyType.AES256_GCM96))
            .toCompletableFuture()
            .join()
    }
}

private fun wrap(
    transit: VaultSecretsTransit,
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
