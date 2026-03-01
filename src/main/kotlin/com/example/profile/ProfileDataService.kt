package com.example.profile

import com.example.api.model.Profile
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USERS
import com.example.shared.exceptions.NotFoundException
import com.example.user.UserId
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ApplicationScoped
class ProfileDataService(
    private val dsl: DSLContext,
) {
    fun getProfileByUsername(
        username: String,
        viewerId: UserId?,
    ): Profile {
        val viewerIdValue = viewerId?.value
        val record =
            dsl
                .select(
                    USERS.USERNAME,
                    USERS.BIO,
                    USERS.IMAGE,
                    viewerIdValue?.let {
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

        return Profile()
            .username(record.get(USERS.USERNAME))
            .bio(record.get(USERS.BIO))
            .image(record.get(USERS.IMAGE))
            .following(record.get("following", Int::class.java) > 0)
    }
}
