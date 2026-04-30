package com.example.tools

import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.mac.MacConfig
import com.google.crypto.tink.mac.PredefinedMacParameters
import io.quarkus.vault.client.VaultClient
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransit
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitCreateKeyParams
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitEncryptParams
import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitKeyType
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient
import java.io.ByteArrayOutputStream
import java.net.http.HttpClient
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
            .build()

    val transit = client.secrets().transit()

    ensureTransitMounted(client)
    ensureKekExists(transit)

    val aeadBytes = serialize(KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM))
    val macBytes = serialize(KeysetHandle.generateNew(PredefinedMacParameters.HMAC_SHA256_256BITTAG))

    val wrappedAead = wrap(transit, aeadBytes)
    val wrappedMac = wrap(transit, macBytes)

    println("# ── Runtime env vars ────────────────────────────────────────────────────────")
    println("# Set these in your deployment (k8s Secret, Vault KV, etc.).")
    println("APP_TINK_AEAD_KEYSET=$wrappedAead")
    println("APP_TINK_MAC_KEYSET=$wrappedMac")
    println()
    println("# ── Cold backup ─────────────────────────────────────────────────────────────")
    println("# Cleartext Tink keyset bytes, base64-encoded.")
    println("# Store offline (printed copy, HSM, Shamir split).")
    println("# Required to recover personal data if Vault storage is permanently lost.")
    println("TINK_AEAD_KEYSET_CLEARTEXT=${Base64.getEncoder().encodeToString(aeadBytes)}")
    println("TINK_MAC_KEYSET_CLEARTEXT=${Base64.getEncoder().encodeToString(macBytes)}")
    println("# ─────────────────────────────────────────────────────────────────────────────")
}

private fun ensureTransitMounted(client: VaultClient) {
    val mounts = client.sys().mounts().list().toCompletableFuture().join()
    if ("transit/" !in mounts) {
        client.sys().mounts().enable("transit", "transit", null, null, null).toCompletableFuture().join()
    }
}

private fun ensureKekExists(transit: VaultSecretsTransit) {
    try {
        transit
            .createKey(KEYSET_KEK, VaultSecretsTransitCreateKeyParams().setType(VaultSecretsTransitKeyType.AES256_GCM96))
            .toCompletableFuture()
            .join()
    } catch (_: Exception) {
        // key already exists — that's fine
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
