package com.example.infrastructure.persistence.jooq.profile

import com.example.application.profile.ProfileView
import com.example.application.profile.ProfileViewReader
import com.example.domain.shared.NotFoundException
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ApplicationScoped
class JooqProfileViewReader(
    private val dsl: DSLContext,
) : ProfileViewReader {
    override fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileView {
        val record =
            dsl
                .select(
                    USERS.USERNAME,
                    USERS.BIO,
                    USERS.IMAGE,
                    viewerId?.let {
                        select(count())
                            .from(FOLLOWERS)
                            .where(FOLLOWERS.FOLLOWEE_ID.eq(USERS.ID))
                            .and(FOLLOWERS.FOLLOWER_ID.eq(it))
                            .asField("following")
                    } ?: org.jooq.impl.DSL
                        .`val`(0)
                        .`as`("following"),
                ).from(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOne() ?: throw NotFoundException("Profile not found")

        return ProfileView(
            username = record.get(USERS.USERNAME)!!,
            bio = record.get(USERS.BIO),
            image = record.get(USERS.IMAGE),
            following = record.get("following", Int::class.java) > 0,
        )
    }
}
