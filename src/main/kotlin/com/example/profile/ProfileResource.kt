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
    lateinit var profileQueryService: ProfileQueryService

    @Inject
    lateinit var securityContext: SecurityContext

    override fun getProfileByUsername(username: String): Response {
        val viewerId = securityContext.currentUserId
        val profile = profileQueryService.getProfileByUsername(username, viewerId)

        return Response
            .ok(GetProfileByUsername200Response().profile(profile))
            .build()
    }

    @RolesAllowed("**")
    override fun followUserByUsername(username: String): Response {
        val currentUserId = securityContext.currentUserId!!
        profileService.followUser(currentUserId, username)

        val profile = profileQueryService.getProfileByUsername(username, currentUserId)
        return Response
            .ok(GetProfileByUsername200Response().profile(profile))
            .build()
    }

    @RolesAllowed("**")
    override fun unfollowUserByUsername(username: String): Response {
        val currentUserId = securityContext.currentUserId!!
        profileService.unfollowUser(currentUserId, username)

        val profile = profileQueryService.getProfileByUsername(username, currentUserId)
        return Response
            .ok(GetProfileByUsername200Response().profile(profile))
            .build()
    }
}
