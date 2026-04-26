package com.example.infrastructure.persistence.jooq.user

import com.example.application.outport.UserWriteRepository
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.PasswordHash
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.infrastructure.security.CryptoService
import com.example.jooq.auth.tables.references.PASSWORD
import com.example.jooq.public.tables.references.FAVORITES
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USER
import com.example.jooq.vault.tables.references.PERSON
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.OffsetDateTime

@ApplicationScoped
class JooqUserWriteRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
) : UserWriteRepository {
    override fun nextId(): UserId =
        UserId(
            dsl
                .select(DSL.field("nextval('user_id_seq')", Long::class.java))
                .fetchSingle()
                .value1()!!,
        )

    override fun create(entity: User): User {
        dsl
            .insertInto(USER)
            .set(USER.ID, entity.id.value)
            .set(USER.CREATED_AT, entity.createdAt)
            .set(USER.UPDATED_AT, entity.updatedAt)
            .execute()

        dsl
            .insertInto(PERSON)
            .set(PERSON.USER_ID, entity.id.value)
            .set(PERSON.EMAIL_ENC, crypto.encryptField(entity.email.value))
            .set(PERSON.EMAIL_HASH, crypto.hmacEmail(entity.email.value))
            .set(PERSON.USERNAME_ENC, crypto.encryptField(entity.username.value))
            .set(PERSON.USERNAME_HASH, crypto.hmacUsername(entity.username.value))
            .set(PERSON.BIO_ENC, entity.bio?.let { crypto.encryptField(it) })
            .set(PERSON.IMAGE_ENC, entity.image?.let { crypto.encryptField(it) })
            .set(PERSON.CREATED_AT, entity.createdAt)
            .set(PERSON.UPDATED_AT, entity.updatedAt)
            .execute()

        dsl
            .insertInto(PASSWORD)
            .set(PASSWORD.USER_ID, entity.id.value)
            .set(PASSWORD.HASH, entity.passwordHash.value)
            .set(PASSWORD.CREATED_AT, entity.createdAt)
            .set(PASSWORD.UPDATED_AT, entity.updatedAt)
            .execute()

        return entity
    }

    override fun update(entity: User): User {
        dsl
            .update(USER)
            .set(USER.UPDATED_AT, entity.updatedAt)
            .where(USER.ID.eq(entity.id.value))
            .execute()

        dsl
            .update(PERSON)
            .set(PERSON.EMAIL_ENC, crypto.encryptField(entity.email.value))
            .set(PERSON.EMAIL_HASH, crypto.hmacEmail(entity.email.value))
            .set(PERSON.USERNAME_ENC, crypto.encryptField(entity.username.value))
            .set(PERSON.USERNAME_HASH, crypto.hmacUsername(entity.username.value))
            .set(PERSON.BIO_ENC, entity.bio?.let { crypto.encryptField(it) })
            .set(PERSON.IMAGE_ENC, entity.image?.let { crypto.encryptField(it) })
            .set(PERSON.UPDATED_AT, entity.updatedAt)
            .where(PERSON.USER_ID.eq(entity.id.value))
            .execute()

        dsl
            .update(PASSWORD)
            .set(PASSWORD.HASH, entity.passwordHash.value)
            .set(PASSWORD.UPDATED_AT, entity.updatedAt)
            .where(PASSWORD.USER_ID.eq(entity.id.value))
            .execute()

        return entity
    }

    override fun findById(id: UserId): User? =
        dsl
            .select(
                USER.ID,
                USER.CREATED_AT,
                USER.UPDATED_AT,
                PERSON.EMAIL_ENC,
                PERSON.USERNAME_ENC,
                PERSON.BIO_ENC,
                PERSON.IMAGE_ENC,
                PASSWORD.HASH,
            ).from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .join(PASSWORD)
            .on(PASSWORD.USER_ID.eq(USER.ID))
            .where(USER.ID.eq(id.value))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { toUser(it) }

    override fun findByEmail(email: Email): User? {
        val emailHash = crypto.hmacEmail(email.value)
        return dsl
            .select(
                USER.ID,
                USER.CREATED_AT,
                USER.UPDATED_AT,
                PERSON.EMAIL_ENC,
                PERSON.USERNAME_ENC,
                PERSON.BIO_ENC,
                PERSON.IMAGE_ENC,
                PASSWORD.HASH,
            ).from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .join(PASSWORD)
            .on(PASSWORD.USER_ID.eq(USER.ID))
            .where(PERSON.EMAIL_HASH.eq(emailHash))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { toUser(it) }
    }

    override fun findByUsername(username: Username): User? {
        val usernameHash = crypto.hmacUsername(username.value)
        return dsl
            .select(
                USER.ID,
                USER.CREATED_AT,
                USER.UPDATED_AT,
                PERSON.EMAIL_ENC,
                PERSON.USERNAME_ENC,
                PERSON.BIO_ENC,
                PERSON.IMAGE_ENC,
                PASSWORD.HASH,
            ).from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .join(PASSWORD)
            .on(PASSWORD.USER_ID.eq(USER.ID))
            .where(PERSON.USERNAME_HASH.eq(usernameHash))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { toUser(it) }
    }

    override fun existsByEmail(email: Email): Boolean =
        dsl.fetchExists(
            dsl
                .selectOne()
                .from(PERSON)
                .where(PERSON.EMAIL_HASH.eq(crypto.hmacEmail(email.value))),
        )

    override fun existsByUsername(username: Username): Boolean =
        dsl.fetchExists(
            dsl
                .selectOne()
                .from(PERSON)
                .where(PERSON.USERNAME_HASH.eq(crypto.hmacUsername(username.value))),
        )

    override fun erase(id: UserId) {
        val now = OffsetDateTime.now()
        dsl.deleteFrom(PERSON).where(PERSON.USER_ID.eq(id.value)).execute()
        dsl.deleteFrom(PASSWORD).where(PASSWORD.USER_ID.eq(id.value)).execute()
        dsl
            .deleteFrom(FOLLOWERS)
            .where(FOLLOWERS.FOLLOWER_ID.eq(id.value).or(FOLLOWERS.FOLLOWEE_ID.eq(id.value)))
            .execute()
        dsl.deleteFrom(FAVORITES).where(FAVORITES.USER_ID.eq(id.value)).execute()
        dsl
            .update(USER)
            .set(USER.DELETED_AT, now)
            .set(USER.UPDATED_AT, now)
            .where(USER.ID.eq(id.value))
            .execute()
    }

    private fun toUser(record: org.jooq.Record): User =
        User(
            id = UserId(record.get(USER.ID)!!),
            email = Email(crypto.decryptField(record.get(PERSON.EMAIL_ENC)!!)),
            username = Username(crypto.decryptField(record.get(PERSON.USERNAME_ENC)!!)),
            passwordHash = PasswordHash(record.get(PASSWORD.HASH)!!),
            bio = record.get(PERSON.BIO_ENC)?.let { crypto.decryptField(it) },
            image = record.get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(it) },
            createdAt = record.get(USER.CREATED_AT)!!,
            updatedAt = record.get(USER.UPDATED_AT)!!,
        )
}
