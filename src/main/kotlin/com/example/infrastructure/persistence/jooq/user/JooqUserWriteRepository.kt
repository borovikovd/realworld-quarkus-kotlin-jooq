package com.example.infrastructure.persistence.jooq.user

import com.example.application.outport.Clock
import com.example.application.outport.CryptoService
import com.example.application.outport.UserWriteRepository
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.PasswordHash
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.jooq.auth.tables.references.PASSWORD
import com.example.jooq.public.tables.references.FAVORITES
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USER
import com.example.jooq.vault.tables.references.ENCRYPTION_KEY
import com.example.jooq.vault.tables.references.PERSON
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL

@ApplicationScoped
class JooqUserWriteRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
    private val clock: Clock,
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

        val dek = crypto.generateDek()
        val encKeyId =
            dsl
                .insertInto(ENCRYPTION_KEY)
                .set(ENCRYPTION_KEY.USER_ID, entity.id.value)
                .set(ENCRYPTION_KEY.KEY_CIPHERTEXT, crypto.encryptDek(dek))
                .returning(ENCRYPTION_KEY.ID)
                .fetchOne()!!
                .get(ENCRYPTION_KEY.ID)!!

        dsl
            .insertInto(PERSON)
            .set(PERSON.USER_ID, entity.id.value)
            .set(PERSON.ENCRYPTION_KEY_ID, encKeyId)
            .set(PERSON.EMAIL_ENC, crypto.encryptField(dek, entity.email.value))
            .set(PERSON.EMAIL_HASH, crypto.hmacEmail(entity.email.value))
            .set(PERSON.USERNAME_ENC, crypto.encryptField(dek, entity.username.value))
            .set(PERSON.USERNAME_HASH, crypto.hmacUsername(entity.username.value))
            .set(PERSON.BIO_ENC, entity.bio?.let { crypto.encryptField(dek, it) })
            .set(PERSON.IMAGE_ENC, entity.image?.let { crypto.encryptField(dek, it) })
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
        val keyCiphertext =
            dsl
                .select(ENCRYPTION_KEY.KEY_CIPHERTEXT)
                .from(ENCRYPTION_KEY)
                .where(ENCRYPTION_KEY.USER_ID.eq(entity.id.value))
                .fetchOne()
                ?.get(ENCRYPTION_KEY.KEY_CIPHERTEXT)!!
        val dek = crypto.decryptDek(keyCiphertext)

        dsl
            .update(USER)
            .set(USER.UPDATED_AT, entity.updatedAt)
            .where(USER.ID.eq(entity.id.value))
            .execute()

        dsl
            .update(PERSON)
            .set(PERSON.EMAIL_ENC, crypto.encryptField(dek, entity.email.value))
            .set(PERSON.EMAIL_HASH, crypto.hmacEmail(entity.email.value))
            .set(PERSON.USERNAME_ENC, crypto.encryptField(dek, entity.username.value))
            .set(PERSON.USERNAME_HASH, crypto.hmacUsername(entity.username.value))
            .set(PERSON.BIO_ENC, entity.bio?.let { crypto.encryptField(dek, it) })
            .set(PERSON.IMAGE_ENC, entity.image?.let { crypto.encryptField(dek, it) })
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
                ENCRYPTION_KEY.KEY_CIPHERTEXT,
                PERSON.EMAIL_ENC,
                PERSON.USERNAME_ENC,
                PERSON.BIO_ENC,
                PERSON.IMAGE_ENC,
                PASSWORD.HASH,
            ).from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .join(ENCRYPTION_KEY)
            .on(ENCRYPTION_KEY.USER_ID.eq(USER.ID))
            .join(PASSWORD)
            .on(PASSWORD.USER_ID.eq(USER.ID))
            .where(USER.ID.eq(id.value))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { toUser(it) }

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
        val now = clock.now()
        dsl.deleteFrom(PERSON).where(PERSON.USER_ID.eq(id.value)).execute()
        dsl.deleteFrom(ENCRYPTION_KEY).where(ENCRYPTION_KEY.USER_ID.eq(id.value)).execute()
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

    private fun toUser(record: org.jooq.Record): User {
        val dek = crypto.decryptDek(record.get(ENCRYPTION_KEY.KEY_CIPHERTEXT)!!)
        return User(
            id = UserId(record.get(USER.ID)!!),
            email = Email(crypto.decryptField(dek, record.get(PERSON.EMAIL_ENC)!!)),
            username = Username(crypto.decryptField(dek, record.get(PERSON.USERNAME_ENC)!!)),
            passwordHash = PasswordHash(record.get(PASSWORD.HASH)!!),
            bio = record.get(PERSON.BIO_ENC)?.let { crypto.decryptField(dek, it) },
            image = record.get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(dek, it) },
            createdAt = record.get(USER.CREATED_AT)!!,
            updatedAt = record.get(USER.UPDATED_AT)!!,
        )
    }
}
