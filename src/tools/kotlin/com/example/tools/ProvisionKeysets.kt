package com.example.tools

import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.http.HttpClient
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.Base64

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

    val aeadBytes = serialize(KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM))
    val macBytes = serialize(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))
    val tokenMacBytes = serialize(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))

    val wrappedAead = wrap(transit, aeadBytes)
    val wrappedMac = wrap(transit, macBytes)
    val wrappedTokenMac = wrap(transit, tokenMacBytes)

    println("# ── Runtime env vars ────────────────────────────────────────────────────────")
    println("# Set these in your deployment (k8s Secret, Vault KV, etc.).")
    println("APP_TINK_AEAD_KEYSET=$wrappedAead")
    println("APP_TINK_MAC_KEYSET=$wrappedMac")
    println("APP_TINK_TOKEN_MAC_KEYSET=$wrappedTokenMac")

    writeBackup(aeadBytes, macBytes, tokenMacBytes)
}

private fun writeBackup(
    aeadBytes: ByteArray,
    macBytes: ByteArray,
    tokenMacBytes: ByteArray,
) {
    val file = File("keyset-cold-backup.txt")
    file.writeText(
        buildString {
            appendLine("# Cleartext Tink keyset bytes, base64-encoded.")
            appendLine("# Store offline (printed copy, HSM, Shamir split).")
            appendLine("# Required to recover personal data if Vault storage is permanently lost.")
            appendLine("# DO NOT commit, log, or transmit this file.")
            appendLine("TINK_AEAD_KEYSET_CLEARTEXT=${Base64.getEncoder().encodeToString(aeadBytes)}")
            appendLine("TINK_MAC_KEYSET_CLEARTEXT=${Base64.getEncoder().encodeToString(macBytes)}")
            appendLine("TINK_TOKEN_MAC_KEYSET_CLEARTEXT=${Base64.getEncoder().encodeToString(tokenMacBytes)}")
        },
    )
    try {
        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"))
    } catch (_: UnsupportedOperationException) {
        System.err.println("Warning: POSIX permissions not supported on this OS — secure ${file.absolutePath} manually")
    }
    println()
    println("# ── Cold backup written to: ${file.absolutePath} (mode 600) ────────────────")
    println("# Store it offline. Delete after securing.")
}

private fun ensureTransitMounted(client: VaultClient) {
    val mounts = client.sys().mounts().list().toCompletableFuture().join()
    if ("transit/" !in mounts) {
        client.sys().mounts().enable("transit", "transit", null, null, null).toCompletableFuture().join()
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

private fun serialize(handle: KeysetHandle): ByteArray {
    val bos = ByteArrayOutputStream()
    CleartextKeysetHandle.write(handle, BinaryKeysetWriter.withOutputStream(bos))
    return bos.toByteArray()
}

private fun wrap(
    transit: VaultSecretsTransit,
    bytes: ByteArray,
): String =
    transit
        .encrypt(KEYSET_KEK, VaultSecretsTransitEncryptParams().setPlaintext(bytes))
        .toCompletableFuture()
        .join()
        .ciphertext
