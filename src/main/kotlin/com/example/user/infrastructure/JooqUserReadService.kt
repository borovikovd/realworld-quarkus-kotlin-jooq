package com.example.user.infrastructure

import com.example.application.user.UserReadService
import com.example.application.user.UserSummary
import com.example.domain.shared.NotFoundException
import com.example.jooq.public.tables.references.USERS
import com.example.shared.architecture.ReadService
import com.example.shared.security.JwtService
import org.jooq.DSLContext

@ReadService
class JooqUserReadService(
    private val dsl: DSLContext,
    private val jwtService: JwtService,
) : UserReadService {
    override fun hydrate(id: Long): UserSummary {
        val record =
            dsl
                .select(USERS.ID, USERS.EMAIL, USERS.USERNAME, USERS.BIO, USERS.IMAGE)
                .from(USERS)
                .where(USERS.ID.eq(id))
                .fetchOne() ?: throw NotFoundException("User not found")

        val token =
            jwtService.generateToken(
                record.get(USERS.ID)!!,
                record.get(USERS.EMAIL)!!,
                record.get(USERS.USERNAME)!!,
            )

        return UserSummary(
            email = record.get(USERS.EMAIL)!!,
            token = token,
            username = record.get(USERS.USERNAME)!!,
            bio = record.get(USERS.BIO),
            image = record.get(USERS.IMAGE),
        )
    }
}
