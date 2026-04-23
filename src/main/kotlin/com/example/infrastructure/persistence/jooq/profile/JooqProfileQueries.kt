package com.example.infrastructure.persistence.jooq.profile

import com.example.application.port.inbound.query.GetProfileByUsernameQuery
import com.example.application.port.outbound.ProfileReadModel
import com.example.application.query.ProfileQueries
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ApplicationScoped
class JooqProfileQueries(
    private val dsl: DSLContext,
) : ProfileQueries {
    override fun getProfileByUsername(query: GetProfileByUsernameQuery): ProfileReadModel? =
        dsl
            .select(
                USERS.USERNAME,
                USERS.BIO,
                USERS.IMAGE,
                query.viewerId?.let {
                    select(count())
                        .from(FOLLOWERS)
                        .where(FOLLOWERS.FOLLOWEE_ID.eq(USERS.ID))
                        .and(FOLLOWERS.FOLLOWER_ID.eq(it))
                        .asField("following")
                } ?: org.jooq.impl.DSL
                    .`val`(0)
                    .`as`("following"),
            ).from(USERS)
            .where(USERS.USERNAME.eq(query.username))
            .fetchOne()
            ?.let { record ->
                ProfileReadModel(
                    username = record.get(USERS.USERNAME)!!,
                    bio = record.get(USERS.BIO),
                    image = record.get(USERS.IMAGE),
                    following = record.get("following", Int::class.java) > 0,
                )
            }
}
