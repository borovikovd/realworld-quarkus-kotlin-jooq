package com.example.infrastructure.persistence.jooq.user

import com.example.application.query.UserQueries
import com.example.application.query.readmodel.UserReadModel
import com.example.domain.user.Email
import com.example.domain.user.UserId
import com.example.domain.user.Username
import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext

@ApplicationScoped
class JooqUserQueries(
    private val dsl: DSLContext,
) : UserQueries {
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
