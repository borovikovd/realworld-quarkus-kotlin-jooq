package com.example.infrastructure.persistence.jooq.user

import com.example.application.port.outbound.UserReadModel
import com.example.application.port.outbound.UserReadRepository
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext

@ApplicationScoped
class JooqUserReadRepository(
    private val dsl: DSLContext,
) : UserReadRepository {
    override fun getUserById(id: Long): UserReadModel? =
        dsl
            .select(USERS.ID, USERS.EMAIL, USERS.USERNAME, USERS.BIO, USERS.IMAGE)
            .from(USERS)
            .where(USERS.ID.eq(id))
            .fetchOne()
            ?.let { record ->
                UserReadModel(
                    id = UserId(record.get(USERS.ID)!!),
                    email = Email(record.get(USERS.EMAIL)!!),
                    username = Username(record.get(USERS.USERNAME)!!),
                    bio = record.get(USERS.BIO),
                    image = record.get(USERS.IMAGE),
                )
            }
}
