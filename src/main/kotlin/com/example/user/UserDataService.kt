package com.example.user

import com.example.jooq.public.tables.references.USERS
import com.example.shared.exceptions.NotFoundException
import com.example.shared.security.JwtService
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import com.example.api.model.User as ApiUser

@ApplicationScoped
class UserDataService(
    private val dsl: DSLContext,
    private val jwtService: JwtService,
) {
    fun hydrate(id: UserId): ApiUser {
        val record =
            dsl
                .select(USERS.ID, USERS.EMAIL, USERS.USERNAME, USERS.BIO, USERS.IMAGE)
                .from(USERS)
                .where(USERS.ID.eq(id.value))
                .fetchOne() ?: throw NotFoundException("User not found")

        val token =
            jwtService.generateToken(
                record.get(USERS.ID)!!,
                record.get(USERS.EMAIL)!!,
                record.get(USERS.USERNAME)!!,
            )

        return ApiUser()
            .email(record.get(USERS.EMAIL))
            .token(token)
            .username(record.get(USERS.USERNAME))
            .bio(record.get(USERS.BIO))
            .image(record.get(USERS.IMAGE))
    }
}
