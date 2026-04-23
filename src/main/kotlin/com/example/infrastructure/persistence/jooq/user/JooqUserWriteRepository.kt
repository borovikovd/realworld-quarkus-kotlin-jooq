package com.example.infrastructure.persistence.jooq.user

import com.example.application.port.outbound.UserWriteRepository
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.PasswordHash
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL

@ApplicationScoped
class JooqUserWriteRepository(
    private val dsl: DSLContext,
) : UserWriteRepository {
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
            .set(USERS.EMAIL, entity.email.value)
            .set(USERS.USERNAME, entity.username.value)
            .set(USERS.PASSWORD_HASH, entity.passwordHash.value)
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
            .set(USERS.EMAIL, entity.email.value)
            .set(USERS.USERNAME, entity.username.value)
            .set(USERS.PASSWORD_HASH, entity.passwordHash.value)
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

    override fun findByEmail(email: Email): User? {
        val record =
            dsl
                .selectFrom(USERS)
                .where(USERS.EMAIL.eq(email.value))
                .fetchOne() ?: return null

        return toUser(record)
    }

    override fun findByUsername(username: Username): User? {
        val record =
            dsl
                .selectFrom(USERS)
                .where(USERS.USERNAME.eq(username.value))
                .fetchOne() ?: return null

        return toUser(record)
    }

    private fun toUser(record: com.example.jooq.public.tables.records.UsersRecord): User =
        User(
            id = UserId(record.id!!),
            email = Email(record.email!!),
            username = Username(record.username!!),
            passwordHash = PasswordHash(record.passwordHash!!),
            bio = record.bio,
            image = record.image,
            createdAt = record.createdAt!!,
            updatedAt = record.updatedAt!!,
        )

    override fun existsByEmail(email: Email): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(USERS)
                .where(USERS.EMAIL.eq(email.value)),
        )

    override fun existsByUsername(username: Username): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(USERS)
                .where(USERS.USERNAME.eq(username.value)),
        )
}
