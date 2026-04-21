package com.example.infrastructure.persistence.jooq.user

import com.example.domain.shared.NotFoundException
import com.example.domain.user.Email
import com.example.domain.user.UserId
import com.example.domain.user.Username
import com.example.domain.user.readmodel.UserView
import com.example.domain.user.readmodel.UserViewReader
import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext

@ApplicationScoped
class JooqUserViewReader(
    private val dsl: DSLContext,
) : UserViewReader {
    override fun getUserById(id: Long): UserView {
        val record =
            dsl
                .select(USERS.ID, USERS.EMAIL, USERS.USERNAME, USERS.BIO, USERS.IMAGE)
                .from(USERS)
                .where(USERS.ID.eq(id))
                .fetchOne() ?: throw NotFoundException("User not found")

        return UserView(
            id = UserId(record.get(USERS.ID)!!),
            email = Email(record.get(USERS.EMAIL)!!),
            username = Username(record.get(USERS.USERNAME)!!),
            bio = record.get(USERS.BIO),
            image = record.get(USERS.IMAGE),
        )
    }
}
