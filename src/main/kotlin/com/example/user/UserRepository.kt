package com.example.user

import com.example.common.persistence.req
import com.example.common.security.PasswordHash
import com.example.jooq.public.tables.references.ARTICLE
import com.example.jooq.public.tables.references.COMMENT
import com.example.jooq.public.tables.references.FAVORITE
import com.example.jooq.public.tables.references.FOLLOWER
import com.example.jooq.public.tables.references.USER
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select
import java.time.OffsetDateTime

@ApplicationScoped
class UserRepository(
    private val dsl: DSLContext,
) {
    fun nextId(): UserId =
        UserId(dsl.select(DSL.field("nextval('user_id_seq')", Long::class.java)).fetchSingle().value1()!!)

    fun insert(user: User) {
        val now = user.createdAt
        dsl
            .insertInto(USER)
            .set(USER.ID, user.id.value)
            .set(USER.EMAIL, user.email)
            .set(USER.USERNAME, user.username)
            .set(USER.PASSWORD_HASH, user.passwordHash.value)
            .set(USER.BIO, user.bio)
            .set(USER.IMAGE, user.image)
            .set(USER.CREATED_AT, now)
            .set(USER.UPDATED_AT, now)
            .execute()
    }

    fun update(user: User) {
        dsl
            .update(USER)
            .set(USER.EMAIL, user.email)
            .set(USER.USERNAME, user.username)
            .set(USER.PASSWORD_HASH, user.passwordHash.value)
            .set(USER.BIO, user.bio)
            .set(USER.IMAGE, user.image)
            .set(USER.UPDATED_AT, user.updatedAt)
            .where(USER.ID.eq(user.id.value))
            .execute()
    }

    fun findById(id: UserId): User? =
        dsl
            .selectFrom(USER)
            .where(USER.ID.eq(id.value))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { toUser(it) }

    fun findByEmail(email: String): User? =
        dsl
            .selectFrom(USER)
            .where(USER.EMAIL.eq(email))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { toUser(it) }

    fun findUserIdByUsername(username: String): UserId? =
        dsl
            .select(USER.ID)
            .from(USER)
            .where(USER.USERNAME.eq(username))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { UserId(it.req(USER.ID)) }

    fun existsByEmail(email: String): Boolean =
        dsl.fetchExists(
            dsl
                .selectOne()
                .from(USER)
                .where(USER.EMAIL.eq(email))
                .and(USER.DELETED_AT.isNull),
        )

    fun existsByUsername(username: String): Boolean =
        dsl.fetchExists(
            dsl
                .selectOne()
                .from(USER)
                .where(USER.USERNAME.eq(username))
                .and(USER.DELETED_AT.isNull),
        )

    fun delete(id: UserId) {
        val now = OffsetDateTime.now()
        dsl
            .deleteFrom(FOLLOWER)
            .where(FOLLOWER.FOLLOWER_ID.eq(id.value).or(FOLLOWER.FOLLOWEE_ID.eq(id.value)))
            .execute()
        dsl.deleteFrom(FAVORITE).where(FAVORITE.USER_ID.eq(id.value)).execute()
        dsl.deleteFrom(COMMENT).where(COMMENT.AUTHOR_ID.eq(id.value)).execute()
        dsl.deleteFrom(ARTICLE).where(ARTICLE.AUTHOR_ID.eq(id.value)).execute()
        dsl
            .update(USER)
            .set(USER.DELETED_AT, now)
            .set(USER.UPDATED_AT, now)
            .where(USER.ID.eq(id.value))
            .execute()
    }

    fun findProfile(
        username: String,
        viewerId: UserId?,
    ): ProfileDto? =
        dsl
            .select(
                USER.USERNAME,
                USER.BIO,
                USER.IMAGE,
                viewerId?.let {
                    select(count())
                        .from(FOLLOWER)
                        .where(FOLLOWER.FOLLOWEE_ID.eq(USER.ID))
                        .and(FOLLOWER.FOLLOWER_ID.eq(it.value))
                        .asField("following")
                } ?: DSL.`val`(0).`as`("following"),
            ).from(USER)
            .where(USER.USERNAME.eq(username))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { record ->
                ProfileDto(
                    username = record.req(USER.USERNAME),
                    bio = record.get(USER.BIO),
                    image = record.get(USER.IMAGE),
                    following = record.get("following", Int::class.java) > 0,
                )
            }

    fun follow(
        followerId: UserId,
        followeeId: UserId,
    ) {
        dsl
            .insertInto(FOLLOWER)
            .set(FOLLOWER.FOLLOWER_ID, followerId.value)
            .set(FOLLOWER.FOLLOWEE_ID, followeeId.value)
            .onDuplicateKeyIgnore()
            .execute()
    }

    fun unfollow(
        followerId: UserId,
        followeeId: UserId,
    ) {
        dsl
            .deleteFrom(FOLLOWER)
            .where(FOLLOWER.FOLLOWER_ID.eq(followerId.value))
            .and(FOLLOWER.FOLLOWEE_ID.eq(followeeId.value))
            .execute()
    }

    private fun toUser(record: org.jooq.Record): User =
        User(
            id = UserId(record.req(USER.ID)),
            email = record.req(USER.EMAIL),
            username = record.req(USER.USERNAME),
            passwordHash = PasswordHash(record.req(USER.PASSWORD_HASH)),
            bio = record.get(USER.BIO),
            image = record.get(USER.IMAGE),
            createdAt = record.req(USER.CREATED_AT),
            updatedAt = record.req(USER.UPDATED_AT),
        )
}
