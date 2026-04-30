@file:Suppress("TooGenericExceptionCaught")

package com.example.infrastructure.security

import com.example.application.outport.CryptoService
import jakarta.annotation.Priority
import jakarta.decorator.Decorator
import jakarta.decorator.Delegate
import jakarta.enterprise.inject.Any
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import org.slf4j.MDC

@Decorator
@Priority(1)
class AuditingCryptoService : CryptoService {
    @Inject
    @Delegate
    @Any
    private lateinit var delegate: CryptoService

    override fun hmacEmail(email: String): String = delegate.hmacEmail(email)

    override fun hmacUsername(username: String): String = delegate.hmacUsername(username)

    override fun hmacRefreshToken(token: String): String = delegate.hmacRefreshToken(token)

    override fun encryptField(
        userId: Long,
        field: String,
        plaintext: String,
    ): ByteArray {
        MDC.put("audit.userId", userId.toString())
        MDC.put("audit.field", field)
        MDC.put("audit.op", "encryptField")
        return try {
            delegate.encryptField(userId, field, plaintext).also { log.info("pii-write") }
        } catch (e: Exception) {
            MDC.put("audit.error", e.javaClass.simpleName)
            log.warn("pii-write-failed")
            throw e
        } finally {
            MDC.remove("audit.error")
            MDC.remove("audit.op")
            MDC.remove("audit.field")
            MDC.remove("audit.userId")
        }
    }

    override fun decryptField(
        userId: Long,
        field: String,
        ciphertext: ByteArray,
    ): String {
        MDC.put("audit.userId", userId.toString())
        MDC.put("audit.field", field)
        MDC.put("audit.op", "decryptField")
        return try {
            delegate.decryptField(userId, field, ciphertext).also { log.info("pii-access") }
        } catch (e: Exception) {
            MDC.put("audit.error", e.javaClass.simpleName)
            log.warn("pii-access-failed")
            throw e
        } finally {
            MDC.remove("audit.error")
            MDC.remove("audit.op")
            MDC.remove("audit.field")
            MDC.remove("audit.userId")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuditingCryptoService::class.java)
    }
}
