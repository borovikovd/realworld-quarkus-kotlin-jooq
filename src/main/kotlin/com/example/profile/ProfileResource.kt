package com.example.profile

import com.example.api.ProfileApi
import com.example.api.model.GetProfileByUsername200Response
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response

@Path("/api")
@ApplicationScoped
class ProfileResource : ProfileApi {
    @Inject
    lateinit var profileService: ProfileService

    @Inject
    lateinit var profileQueries: ProfileQueries

    @Inject
    lateinit var securityContext: SecurityContext

    override fun getProfileByUsername(username: String): Response {
        val viewerId = securityContext.currentUserId
        val profile = profileQueries.getProfileByUsername(username, viewerId)

        return Response
            .ok(GetProfileByUsername200Response().profile(profile))
            .build()
    }

    @RolesAllowed("user")
    override fun followUserByUsername(username: String): Response {
        profileService.followUser(username)

        val currentUserId = securityContext.currentUserId!!
        val profile = profileQueries.getProfileByUsername(username, currentUserId)
        return Response
            .ok(GetProfileByUsername200Response().profile(profile))
            .build()
    }

    @RolesAllowed("user")
    override fun unfollowUserByUsername(username: String): Response {
        profileService.unfollowUser(username)

        val currentUserId = securityContext.currentUserId!!
        val profile = profileQueries.getProfileByUsername(username, currentUserId)
        return Response
            .ok(GetProfileByUsername200Response().profile(profile))
            .build()
    }
}
