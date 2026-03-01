package com.example.user

import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext

@ApplicationScoped
class JooqUserRepository(
    private val dsl: DSLContext,
) : UserRepository {
    override fun create(entity: User): User {
        require(entity.id == null) { "Cannot create entity with existing ID" }

        val id =
            dsl
                .insertInto(USERS)
                .set(USERS.EMAIL, entity.email)
                .set(USERS.USERNAME, entity.username)
                .set(USERS.PASSWORD_HASH, entity.passwordHash)
                .set(USERS.BIO, entity.bio)
                .set(USERS.IMAGE, entity.image)
                .set(USERS.CREATED_AT, entity.createdAt)
                .set(USERS.UPDATED_AT, entity.updatedAt)
                .returningResult(USERS.ID)
                .fetchOne()
                ?.value1() ?: error("Failed to insert user")

        return entity.withId(id)
    }

    override fun update(entity: User): User {
        requireNotNull(entity.id) { "Cannot update entity without ID" }

        dsl
            .update(USERS)
            .set(USERS.EMAIL, entity.email)
            .set(USERS.USERNAME, entity.username)
            .set(USERS.PASSWORD_HASH, entity.passwordHash)
            .set(USERS.BIO, entity.bio)
            .set(USERS.IMAGE, entity.image)
            .set(USERS.UPDATED_AT, entity.updatedAt)
            .where(USERS.ID.eq(entity.id))
            .execute()

        return entity
    }

    override fun findById(id: Long): User? {
        val record =
            dsl
                .selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .fetchOne() ?: return null

        return User(
            id = record.id,
            email = record.email!!,
            username = record.username!!,
            passwordHash = record.passwordHash!!,
            bio = record.bio,
            image = record.image,
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!,
        )
    }

    override fun findByEmail(email: String): User? {
        val record =
            dsl
                .selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOne() ?: return null

        return User(
            id = record.id,
            email = record.email!!,
            username = record.username!!,
            passwordHash = record.passwordHash!!,
            bio = record.bio,
            image = record.image,
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!,
        )
    }

    override fun findByUsername(username: String): User? {
        val record =
            dsl
                .selectFrom(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOne() ?: return null

        return User(
            id = record.id,
            email = record.email!!,
            username = record.username!!,
            passwordHash = record.passwordHash!!,
            bio = record.bio,
            image = record.image,
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!,
        )
    }

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
