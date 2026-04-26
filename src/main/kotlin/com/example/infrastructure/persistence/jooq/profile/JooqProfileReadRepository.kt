package com.example.infrastructure.persistence.jooq.profile

import com.example.application.outport.ProfileReadRepository
import com.example.application.readmodel.ProfileReadModel
import com.example.infrastructure.security.CryptoService
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USER
import com.example.jooq.vault.tables.references.ENCRYPTION_KEY
import com.example.jooq.vault.tables.references.PERSON
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ApplicationScoped
class JooqProfileReadRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
) : ProfileReadRepository {
    override fun findByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel? {
        val usernameHash = crypto.hmacUsername(username)
        return dsl
            .select(
                ENCRYPTION_KEY.KEY_CIPHERTEXT,
                PERSON.USERNAME_ENC,
                PERSON.BIO_ENC,
                PERSON.IMAGE_ENC,
                viewerId?.let {
                    select(count())
                        .from(FOLLOWERS)
                        .where(FOLLOWERS.FOLLOWEE_ID.eq(PERSON.USER_ID))
                        .and(FOLLOWERS.FOLLOWER_ID.eq(it))
                        .asField("following")
                } ?: org.jooq.impl.DSL
                    .`val`(0)
                    .`as`("following"),
            ).from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .join(ENCRYPTION_KEY)
            .on(ENCRYPTION_KEY.USER_ID.eq(USER.ID))
            .where(PERSON.USERNAME_HASH.eq(usernameHash))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { record ->
                val dek = crypto.decryptDek(record.get(ENCRYPTION_KEY.KEY_CIPHERTEXT)!!)
                ProfileReadModel(
                    username = crypto.decryptField(dek, record.get(PERSON.USERNAME_ENC)!!),
                    bio = record.get(PERSON.BIO_ENC)?.let { crypto.decryptField(dek, it) },
                    image = record.get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(dek, it) },
                    following = record.get("following", Int::class.java) > 0,
                )
            }
    }
}
