package com.example.infrastructure.persistence.jooq.user

import com.example.application.user.UserReadService
import com.example.application.user.UserSummary
import com.example.domain.auth.TokenIssuer
import com.example.domain.shared.NotFoundException
import com.example.domain.user.Email
import com.example.domain.user.UserId
import com.example.domain.user.Username
import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext

@ApplicationScoped
class JooqUserReadService(
    private val dsl: DSLContext,
    private val tokenIssuer: TokenIssuer,
) : UserReadService {
    override fun hydrate(id: Long): UserSummary {
        val record =
            dsl
                .select(USERS.ID, USERS.EMAIL, USERS.USERNAME, USERS.BIO, USERS.IMAGE)
                .from(USERS)
                .where(USERS.ID.eq(id))
                .fetchOne() ?: throw NotFoundException("User not found")

        val email = record.get(USERS.EMAIL)!!
        val username = record.get(USERS.USERNAME)!!
        val token =
            tokenIssuer.issue(
                UserId(record.get(USERS.ID)!!),
                Email(email),
                Username(username),
            )

        return UserSummary(
            email = email,
            token = token,
            username = username,
            bio = record.get(USERS.BIO),
            image = record.get(USERS.IMAGE),
        )
    }
}
