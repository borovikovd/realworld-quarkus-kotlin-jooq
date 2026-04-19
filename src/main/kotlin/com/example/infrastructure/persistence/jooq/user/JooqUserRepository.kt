package com.example.infrastructure.persistence.jooq.user

import com.example.domain.user.User
import com.example.domain.user.UserId
import com.example.domain.user.UserRepository
import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL

@ApplicationScoped
class JooqUserRepository(
    private val dsl: DSLContext,
) : UserRepository {
    override fun nextId(): UserId =
        UserId(
            dsl
                .select(DSL.field("nextval('users_id_seq')", Long::class.java))
                .fetchSingle()
                .value1()!!,
        )

    override fun create(entity: User): User {
        dsl
            .insertInto(USERS)
            .set(USERS.ID, entity.id.value)
            .set(USERS.EMAIL, entity.email)
            .set(USERS.USERNAME, entity.username)
            .set(USERS.PASSWORD_HASH, entity.passwordHash)
            .set(USERS.BIO, entity.bio)
            .set(USERS.IMAGE, entity.image)
            .set(USERS.CREATED_AT, entity.createdAt)
            .set(USERS.UPDATED_AT, entity.updatedAt)
            .execute()

        return entity
    }

    override fun update(entity: User): User {
        dsl
            .update(USERS)
            .set(USERS.EMAIL, entity.email)
            .set(USERS.USERNAME, entity.username)
            .set(USERS.PASSWORD_HASH, entity.passwordHash)
            .set(USERS.BIO, entity.bio)
            .set(USERS.IMAGE, entity.image)
            .set(USERS.UPDATED_AT, entity.updatedAt)
            .where(USERS.ID.eq(entity.id.value))
            .execute()

        return entity
    }

    override fun findById(id: UserId): User? {
        val record =
            dsl
                .selectFrom(USERS)
                .where(USERS.ID.eq(id.value))
                .fetchOne() ?: return null

        return toUser(record)
    }

    override fun findByEmail(email: String): User? {
        val record =
            dsl
                .selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOne() ?: return null

        return toUser(record)
    }

    override fun findByUsername(username: String): User? {
        val record =
            dsl
                .selectFrom(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOne() ?: return null

        return toUser(record)
    }

    private fun toUser(record: com.example.jooq.public.tables.records.UsersRecord): User =
        User(
            id = UserId(record.id!!),
            email = record.email!!,
            username = record.username!!,
            passwordHash = record.passwordHash!!,
            bio = record.bio,
            image = record.image,
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!,
        )

    override fun existsByEmail(email: String): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(USERS)
                .where(USERS.EMAIL.eq(email)),
        )

    override fun existsByUsername(username: String): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(USERS)
                .where(USERS.USERNAME.eq(username)),
        )
}
