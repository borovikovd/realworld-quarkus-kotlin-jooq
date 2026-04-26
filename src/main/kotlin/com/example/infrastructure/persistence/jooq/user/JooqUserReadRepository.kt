package com.example.infrastructure.persistence.jooq.user

import com.example.application.outport.UserReadRepository
import com.example.application.readmodel.UserReadModel
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.infrastructure.security.CryptoService
import com.example.jooq.public.tables.references.USER
import com.example.jooq.vault.tables.references.PERSON
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext

@ApplicationScoped
class JooqUserReadRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
) : UserReadRepository {
    override fun findById(id: Long): UserReadModel? =
        dsl
            .select(
                USER.ID,
                PERSON.EMAIL_ENC,
                PERSON.USERNAME_ENC,
                PERSON.BIO_ENC,
                PERSON.IMAGE_ENC,
            ).from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .where(USER.ID.eq(id))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { record ->
                UserReadModel(
                    id = UserId(record.get(USER.ID)!!),
                    email = Email(crypto.decryptField(record.get(PERSON.EMAIL_ENC)!!)),
                    username = Username(crypto.decryptField(record.get(PERSON.USERNAME_ENC)!!)),
                    bio = record.get(PERSON.BIO_ENC)?.let { crypto.decryptField(it) },
                    image = record.get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(it) },
                )
            }
}
