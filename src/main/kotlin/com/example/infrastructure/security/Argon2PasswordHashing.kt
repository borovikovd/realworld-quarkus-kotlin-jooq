package com.example.infrastructure.security

import com.example.application.outport.PasswordHashing
import com.example.domain.aggregate.user.PasswordHash
import jakarta.enterprise.context.ApplicationScoped
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@ApplicationScoped
class Argon2PasswordHashing : PasswordHashing {
    private val secureRandom = SecureRandom()
    private val base64Encoder = Base64.getEncoder().withoutPadding()
    private val base64Decoder = Base64.getDecoder()

    override fun hash(raw: String): PasswordHash {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val hash = generateHash(raw, salt, ITERATIONS, MEMORY_KB, PARALLELISM)

        val encodedSalt = base64Encoder.encodeToString(salt)
        val encodedHash = base64Encoder.encodeToString(hash)
        return PasswordHash(
            "\$argon2id\$v=19\$m=$MEMORY_KB,t=$ITERATIONS,p=$PARALLELISM\$$encodedSalt\$$encodedHash",
        )
    }

    override fun verify(
        hash: PasswordHash,
        raw: String,
    ): Boolean {
        val parsed = parseEncoded(hash.value) ?: return false
        val computed = generateHash(raw, parsed.salt, parsed.iterations, parsed.memory, parsed.parallelism)
        return MessageDigest.isEqual(parsed.hash, computed)
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

    companion object {
        private const val SALT_LENGTH = 32
        private const val HASH_LENGTH = 64
        private const val ITERATIONS = 3
        private const val MEMORY_KB = 65536
        private const val PARALLELISM = 1
        private const val ENCODED_PARTS_COUNT = 5
        private const val PARAMS_INDEX = 2
        private const val SALT_INDEX = 3
        private const val HASH_INDEX = 4
    }
}
