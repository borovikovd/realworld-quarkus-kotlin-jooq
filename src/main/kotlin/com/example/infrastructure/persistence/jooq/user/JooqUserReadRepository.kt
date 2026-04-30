package com.example.infrastructure.persistence.jooq.user

import com.example.application.outport.CryptoService
import com.example.application.outport.UserReadRepository
import com.example.application.readmodel.LoginCredentials
import com.example.application.readmodel.UserReadModel
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.PasswordHash
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.jooq.auth.tables.references.PASSWORD
import com.example.jooq.public.tables.references.USER
import com.example.jooq.vault.tables.references.PERSON
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext

@ApplicationScoped
class JooqUserReadRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
) : UserReadRepository {
    override fun findById(id: Long): UserReadModel? =
        dsl
            .select(
                USER.ID,
                PERSON.EMAIL_ENC,
                PERSON.USERNAME_ENC,
                PERSON.BIO_ENC,
                PERSON.IMAGE_ENC,
            ).from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .where(USER.ID.eq(id))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { record ->
                val userId = record.get(USER.ID)!!
                val emailEnc = record.get(PERSON.EMAIL_ENC)!!
                val usernameEnc = record.get(PERSON.USERNAME_ENC)!!
                UserReadModel(
                    id = UserId(userId),
                    email = Email(crypto.decryptField(userId, CryptoService.EMAIL, emailEnc)),
                    username = Username(crypto.decryptField(userId, CryptoService.USERNAME, usernameEnc)),
                    bio = record.get(PERSON.BIO_ENC)?.let { crypto.decryptField(userId, CryptoService.BIO, it) },
                    image = record.get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(userId, CryptoService.IMAGE, it) },
                )
            }

    override fun findCredentialsByEmail(email: Email): LoginCredentials? {
        val emailHash = crypto.hmacEmail(email.value)
        return dsl
            .select(USER.ID, PASSWORD.HASH)
            .from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .join(PASSWORD)
            .on(PASSWORD.USER_ID.eq(USER.ID))
            .where(PERSON.EMAIL_HASH.eq(emailHash))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { record ->
                LoginCredentials(
                    userId = UserId(record.get(USER.ID)!!),
                    passwordHash = PasswordHash(record.get(PASSWORD.HASH)!!),
                )
            }
    }

    override fun findUserIdByUsername(username: String): UserId? {
        val usernameHash = crypto.hmacUsername(username)
        return dsl
            .select(USER.ID)
            .from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .where(PERSON.USERNAME_HASH.eq(usernameHash))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { UserId(it.get(USER.ID)!!) }
    }
}
