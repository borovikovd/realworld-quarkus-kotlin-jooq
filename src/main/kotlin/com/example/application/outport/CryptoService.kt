package com.example.application.outport

interface CryptoService {
    fun hmacEmail(email: String): String

    fun hmacUsername(username: String): String

    fun hmacRefreshToken(token: String): String

    fun encryptField(
        userId: Long,
        plaintext: String,
    ): ByteArray

    fun decryptField(
        userId: Long,
        ciphertext: ByteArray,
    ): String
}
