package com.example.shared.security

import jakarta.enterprise.context.ApplicationScoped
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@ApplicationScoped
class PasswordHasher {
    companion object {
        private const val SALT_LENGTH = 32
        private const val HASH_LENGTH = 64
        private const val ITERATIONS = 10
        private const val MEMORY_KB = 65536
        private const val PARALLELISM = 1
        private const val ENCODED_PARTS_COUNT = 5
        private const val PARAMS_INDEX = 2
        private const val SALT_INDEX = 3
        private const val HASH_INDEX = 4
    }

    private val secureRandom = SecureRandom()
    private val base64Encoder = Base64.getEncoder().withoutPadding()
    private val base64Decoder = Base64.getDecoder()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val hash = generateHash(password, salt, ITERATIONS, MEMORY_KB, PARALLELISM)

        val encodedSalt = base64Encoder.encodeToString(salt)
        val encodedHash = base64Encoder.encodeToString(hash)
        return "\$argon2id\$v=19\$m=$MEMORY_KB,t=$ITERATIONS,p=$PARALLELISM\$$encodedSalt\$$encodedHash"
    }

    fun verify(
        hash: String,
        password: String,
    ): Boolean {
        val parsed = parseEncoded(hash) ?: return false
        val computedHash = generateHash(password, parsed.salt, parsed.iterations, parsed.memory, parsed.parallelism)
        return MessageDigest.isEqual(parsed.hash, computedHash)
    }

    private data class ParsedHash(
        val memory: Int,
        val iterations: Int,
        val parallelism: Int,
        val salt: ByteArray,
        val hash: ByteArray,
    )

    private fun parseEncoded(encoded: String): ParsedHash? {
        val parts = encoded.split("$").filter { it.isNotEmpty() }
        if (parts.size != ENCODED_PARTS_COUNT || parts[0] != "argon2id") return null

        val paramMap =
            parts[PARAMS_INDEX].split(",").associate {
                val (key, value) = it.split("=")
                key to value.toInt()
            }

        val memory = paramMap["m"]
        val iterations = paramMap["t"]
        val parallelism = paramMap["p"]

        return if (memory != null && iterations != null && parallelism != null) {
            ParsedHash(
                memory = memory,
                iterations = iterations,
                parallelism = parallelism,
                salt = base64Decoder.decode(parts[SALT_INDEX]),
                hash = base64Decoder.decode(parts[HASH_INDEX]),
            )
        } else {
            null
        }
    }

    private fun generateHash(
        password: String,
        salt: ByteArray,
        iterations: Int,
        memoryKb: Int,
        parallelism: Int,
    ): ByteArray {
        val params =
            Argon2Parameters
                .Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iterations)
                .withMemoryAsKB(memoryKb)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)

        val result = ByteArray(HASH_LENGTH)
        generator.generateBytes(password.toCharArray(), result)
        return result
    }
}
