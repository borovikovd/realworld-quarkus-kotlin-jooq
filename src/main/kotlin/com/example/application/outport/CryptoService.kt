package com.example.application.outport

interface CryptoService {
    fun hmacEmail(email: String): String

    fun hmacUsername(username: String): String

    fun hmacRefreshToken(token: String): String

    fun generateDek(): ByteArray

    fun encryptDek(dek: ByteArray): String

    fun decryptDek(keyCiphertext: String): ByteArray

    fun encryptField(
        dek: ByteArray,
        plaintext: String,
    ): ByteArray

    fun decryptField(
        dek: ByteArray,
        ciphertext: ByteArray,
    ): String
}
