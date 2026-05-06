package com.example.application.port.security

interface CryptoService {
    fun hmacEmail(email: String): String

    fun hmacUsername(username: String): String

    fun hmacRefreshToken(token: String): String

    fun encryptField(
        userId: Long,
        field: String,
        plaintext: String,
    ): ByteArray

    fun decryptField(
        userId: Long,
        field: String,
        ciphertext: ByteArray,
    ): String
}
