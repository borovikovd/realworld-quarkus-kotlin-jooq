package com.example.infrastructure.persistence.jooq

import com.example.application.port.Clock
import com.example.application.port.UserRepository
import com.example.application.port.security.CryptoService
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.PasswordHash
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.infrastructure.persistence.jooq.shared.FIELD_BIO
import com.example.infrastructure.persistence.jooq.shared.FIELD_EMAIL
import com.example.infrastructure.persistence.jooq.shared.FIELD_IMAGE
import com.example.infrastructure.persistence.jooq.shared.FIELD_USERNAME
import com.example.jooq.auth.tables.references.PASSWORD
import com.example.jooq.public.tables.references.ARTICLES
import com.example.jooq.public.tables.references.COMMENTS
import com.example.jooq.public.tables.references.FAVORITES
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USER
import com.example.jooq.vault.tables.references.PERSON
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL

@ApplicationScoped
class JooqUserRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
    private val clock: Clock,
) : UserRepository {
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

        val userId = entity.id.value
        dsl
            .insertInto(PERSON)
            .set(PERSON.USER_ID, userId)
            .set(PERSON.EMAIL_ENC, crypto.encryptField(userId, FIELD_EMAIL, entity.email.value))
            .set(PERSON.EMAIL_HASH, crypto.hmacEmail(entity.email.value))
            .set(PERSON.USERNAME_ENC, crypto.encryptField(userId, FIELD_USERNAME, entity.username.value))
            .set(PERSON.USERNAME_HASH, crypto.hmacUsername(entity.username.value))
            .set(PERSON.BIO_ENC, entity.bio?.let { crypto.encryptField(userId, FIELD_BIO, it) })
            .set(PERSON.IMAGE_ENC, entity.image?.let { crypto.encryptField(userId, FIELD_IMAGE, it) })
            .set(PERSON.CREATED_AT, entity.createdAt)
            .set(PERSON.UPDATED_AT, entity.updatedAt)
            .execute()

        dsl
            .insertInto(PASSWORD)
            .set(PASSWORD.USER_ID, userId)
            .set(PASSWORD.HASH, entity.passwordHash.value)
            .set(PASSWORD.CREATED_AT, entity.createdAt)
            .set(PASSWORD.UPDATED_AT, entity.updatedAt)
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

    override fun update(entity: User): User {
        val userId = entity.id.value

        dsl
            .update(USER)
            .set(USER.UPDATED_AT, entity.updatedAt)
            .where(USER.ID.eq(userId))
            .execute()

        dsl
            .update(PERSON)
            .set(PERSON.EMAIL_ENC, crypto.encryptField(userId, FIELD_EMAIL, entity.email.value))
            .set(PERSON.EMAIL_HASH, crypto.hmacEmail(entity.email.value))
            .set(PERSON.USERNAME_ENC, crypto.encryptField(userId, FIELD_USERNAME, entity.username.value))
            .set(PERSON.USERNAME_HASH, crypto.hmacUsername(entity.username.value))
            .set(PERSON.BIO_ENC, entity.bio?.let { crypto.encryptField(userId, FIELD_BIO, it) })
            .set(PERSON.IMAGE_ENC, entity.image?.let { crypto.encryptField(userId, FIELD_IMAGE, it) })
            .set(PERSON.UPDATED_AT, entity.updatedAt)
            .where(PERSON.USER_ID.eq(userId))
            .execute()

        dsl
            .update(PASSWORD)
            .set(PASSWORD.HASH, entity.passwordHash.value)
            .set(PASSWORD.UPDATED_AT, entity.updatedAt)
            .where(PASSWORD.USER_ID.eq(userId))
            .execute()

        return entity
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
        val now = clock.now()
        dsl.deleteFrom(PERSON).where(PERSON.USER_ID.eq(id.value)).execute()
        dsl.deleteFrom(PASSWORD).where(PASSWORD.USER_ID.eq(id.value)).execute()
        dsl
            .deleteFrom(FOLLOWERS)
            .where(FOLLOWERS.FOLLOWER_ID.eq(id.value).or(FOLLOWERS.FOLLOWEE_ID.eq(id.value)))
            .execute()
        dsl.deleteFrom(FAVORITES).where(FAVORITES.USER_ID.eq(id.value)).execute()
        // Delete comments by this user on other users' articles (own-article comments cascade below)
        dsl.deleteFrom(COMMENTS).where(COMMENTS.AUTHOR_ID.eq(id.value)).execute()
        // Delete articles; cascades article_tags, favorites, and comments on those articles
        dsl.deleteFrom(ARTICLES).where(ARTICLES.AUTHOR_ID.eq(id.value)).execute()
        dsl
            .update(USER)
            .set(USER.DELETED_AT, now)
            .set(USER.UPDATED_AT, now)
            .where(USER.ID.eq(id.value))
            .execute()
    }

    private fun toUser(record: org.jooq.Record): User {
        val userId = record.get(USER.ID)!!
        return User(
            id = UserId(userId),
            email = Email(crypto.decryptField(userId, FIELD_EMAIL, record.get(PERSON.EMAIL_ENC)!!)),
            username = Username(crypto.decryptField(userId, FIELD_USERNAME, record.get(PERSON.USERNAME_ENC)!!)),
            passwordHash = PasswordHash(record.get(PASSWORD.HASH)!!),
            bio = record.get(PERSON.BIO_ENC)?.let { crypto.decryptField(userId, FIELD_BIO, it) },
            image = record.get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(userId, FIELD_IMAGE, it) },
            createdAt = record.get(USER.CREATED_AT)!!,
            updatedAt = record.get(USER.UPDATED_AT)!!,
        )
    }
}
