package com.example.user

import com.example.common.persistence.FIELD_BIO
import com.example.common.persistence.FIELD_EMAIL
import com.example.common.persistence.FIELD_IMAGE
import com.example.common.persistence.FIELD_USERNAME
import com.example.common.persistence.decryptAuthorProfile
import com.example.common.persistence.req
import com.example.common.security.CryptoService
import com.example.common.security.PasswordHash
import com.example.common.time.Clock
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
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

interface UserRepository {
    fun nextId(): UserId

    fun insert(user: User)

    fun update(user: User)

    fun findById(id: UserId): User?

    fun findByEmail(email: String): User?

    fun findUserIdByUsername(username: String): UserId?

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean

    fun erase(id: UserId)

    fun findProfile(
        username: String,
        viewerId: UserId?,
    ): ProfileDto?

    fun follow(
        followerId: UserId,
        followeeId: UserId,
    )

    fun unfollow(
        followerId: UserId,
        followeeId: UserId,
    )
}

@ApplicationScoped
class JooqUserRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
    private val clock: Clock,
) : UserRepository {
    override fun nextId(): UserId =
        UserId(dsl.select(DSL.field("nextval('user_id_seq')", Long::class.java)).fetchSingle().value1()!!)

    override fun insert(user: User) {
        val userId = user.id.value
        val now = user.createdAt

        dsl
            .insertInto(USER)
            .set(USER.ID, userId)
            .set(USER.CREATED_AT, now)
            .set(USER.UPDATED_AT, now)
            .execute()

        dsl
            .insertInto(PERSON)
            .set(PERSON.USER_ID, userId)
            .set(PERSON.EMAIL_ENC, crypto.encryptField(userId, FIELD_EMAIL, user.email))
            .set(PERSON.EMAIL_HASH, crypto.hmacEmail(user.email))
            .set(PERSON.USERNAME_ENC, crypto.encryptField(userId, FIELD_USERNAME, user.username))
            .set(PERSON.USERNAME_HASH, crypto.hmacUsername(user.username))
            .set(PERSON.BIO_ENC, user.bio?.let { crypto.encryptField(userId, FIELD_BIO, it) })
            .set(PERSON.IMAGE_ENC, user.image?.let { crypto.encryptField(userId, FIELD_IMAGE, it) })
            .set(PERSON.CREATED_AT, now)
            .set(PERSON.UPDATED_AT, now)
            .execute()

        dsl
            .insertInto(PASSWORD)
            .set(PASSWORD.USER_ID, userId)
            .set(PASSWORD.HASH, user.passwordHash.value)
            .set(PASSWORD.CREATED_AT, now)
            .set(PASSWORD.UPDATED_AT, now)
            .execute()
    }

    override fun update(user: User) {
        val userId = user.id.value

        dsl
            .update(USER)
            .set(USER.UPDATED_AT, user.updatedAt)
            .where(USER.ID.eq(userId))
            .execute()

        dsl
            .update(PERSON)
            .set(PERSON.EMAIL_ENC, crypto.encryptField(userId, FIELD_EMAIL, user.email))
            .set(PERSON.EMAIL_HASH, crypto.hmacEmail(user.email))
            .set(PERSON.USERNAME_ENC, crypto.encryptField(userId, FIELD_USERNAME, user.username))
            .set(PERSON.USERNAME_HASH, crypto.hmacUsername(user.username))
            .set(PERSON.BIO_ENC, user.bio?.let { crypto.encryptField(userId, FIELD_BIO, it) })
            .set(PERSON.IMAGE_ENC, user.image?.let { crypto.encryptField(userId, FIELD_IMAGE, it) })
            .set(PERSON.UPDATED_AT, user.updatedAt)
            .where(PERSON.USER_ID.eq(userId))
            .execute()

        dsl
            .update(PASSWORD)
            .set(PASSWORD.HASH, user.passwordHash.value)
            .set(PASSWORD.UPDATED_AT, user.updatedAt)
            .where(PASSWORD.USER_ID.eq(userId))
            .execute()
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

    override fun findByEmail(email: String): User? {
        val emailHash = crypto.hmacEmail(email)
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
            ?.let { UserId(it.req(USER.ID)) }
    }

    override fun existsByEmail(email: String): Boolean =
        dsl.fetchExists(dsl.selectOne().from(PERSON).where(PERSON.EMAIL_HASH.eq(crypto.hmacEmail(email))))

    override fun existsByUsername(username: String): Boolean =
        dsl.fetchExists(dsl.selectOne().from(PERSON).where(PERSON.USERNAME_HASH.eq(crypto.hmacUsername(username))))

    override fun erase(id: UserId) {
        val now = clock.now()
        dsl.deleteFrom(PERSON).where(PERSON.USER_ID.eq(id.value)).execute()
        dsl.deleteFrom(PASSWORD).where(PASSWORD.USER_ID.eq(id.value)).execute()
        dsl
            .deleteFrom(FOLLOWERS)
            .where(FOLLOWERS.FOLLOWER_ID.eq(id.value).or(FOLLOWERS.FOLLOWEE_ID.eq(id.value)))
            .execute()
        dsl.deleteFrom(FAVORITES).where(FAVORITES.USER_ID.eq(id.value)).execute()
        dsl.deleteFrom(COMMENTS).where(COMMENTS.AUTHOR_ID.eq(id.value)).execute()
        dsl.deleteFrom(ARTICLES).where(ARTICLES.AUTHOR_ID.eq(id.value)).execute()
        dsl
            .update(USER)
            .set(USER.DELETED_AT, now)
            .set(USER.UPDATED_AT, now)
            .where(USER.ID.eq(id.value))
            .execute()
    }

    override fun findProfile(
        username: String,
        viewerId: UserId?,
    ): ProfileDto? {
        val usernameHash = crypto.hmacUsername(username)
        return dsl
            .select(
                USER.ID,
                PERSON.USERNAME_ENC,
                PERSON.BIO_ENC,
                PERSON.IMAGE_ENC,
                viewerId?.let {
                    select(count())
                        .from(FOLLOWERS)
                        .where(FOLLOWERS.FOLLOWEE_ID.eq(PERSON.USER_ID))
                        .and(FOLLOWERS.FOLLOWER_ID.eq(it.value))
                        .asField("following")
                } ?: DSL.`val`(0).`as`("following"),
            ).from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .where(PERSON.USERNAME_HASH.eq(usernameHash))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { record ->
                record.decryptAuthorProfile(
                    crypto = crypto,
                    userId = record.get(USER.ID),
                    following = record.get("following", Int::class.java) > 0,
                )
            }
    }

    override fun follow(
        followerId: UserId,
        followeeId: UserId,
    ) {
        dsl
            .insertInto(FOLLOWERS)
            .set(FOLLOWERS.FOLLOWER_ID, followerId.value)
            .set(FOLLOWERS.FOLLOWEE_ID, followeeId.value)
            .onDuplicateKeyIgnore()
            .execute()
    }

    override fun unfollow(
        followerId: UserId,
        followeeId: UserId,
    ) {
        dsl
            .deleteFrom(FOLLOWERS)
            .where(FOLLOWERS.FOLLOWER_ID.eq(followerId.value))
            .and(FOLLOWERS.FOLLOWEE_ID.eq(followeeId.value))
            .execute()
    }

    private fun toUser(record: org.jooq.Record): User {
        val userId = record.req(USER.ID)
        return User(
            id = UserId(userId),
            email = crypto.decryptField(userId, FIELD_EMAIL, record.req(PERSON.EMAIL_ENC)),
            username = crypto.decryptField(userId, FIELD_USERNAME, record.req(PERSON.USERNAME_ENC)),
            passwordHash = PasswordHash(record.req(PASSWORD.HASH)),
            bio = record.get(PERSON.BIO_ENC)?.let { crypto.decryptField(userId, FIELD_BIO, it) },
            image = record.get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(userId, FIELD_IMAGE, it) },
            createdAt = record.req(USER.CREATED_AT),
            updatedAt = record.req(USER.UPDATED_AT),
        )
    }
}
