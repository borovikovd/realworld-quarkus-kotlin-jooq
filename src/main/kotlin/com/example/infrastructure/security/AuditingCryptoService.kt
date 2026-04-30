package com.example.infrastructure.security

import com.example.application.outport.CryptoService
import jakarta.annotation.Priority
import jakarta.decorator.Decorator
import jakarta.decorator.Delegate
import jakarta.enterprise.inject.Any
import jakarta.inject.Inject
import org.jboss.logging.Logger
import org.jboss.logging.MDC

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
        plaintext: String,
    ): ByteArray = delegate.encryptField(userId, plaintext)

    override fun decryptField(
        userId: Long,
        ciphertext: ByteArray,
    ): String {
        MDC.put("audit.userId", userId.toString())
        MDC.put("audit.op", "decryptField")
        log.info("pii-access")
        MDC.remove("audit.op")
        MDC.remove("audit.userId")
        return delegate.decryptField(userId, ciphertext)
    }

    companion object {
        private val log: Logger = Logger.getLogger(AuditingCryptoService::class.java)
    }
}
