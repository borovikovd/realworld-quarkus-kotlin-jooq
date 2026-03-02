package com.example.profile

import com.example.api.ProfileApi
import com.example.api.model.GetProfileByUsername200Response
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response

@ApplicationScoped
class ProfileResource(
    private val profileWriteService: ProfileWriteService,
    private val profileReadService: ProfileReadService,
    private val securityContext: SecurityContext,
) : ProfileApi {
    override fun getProfileByUsername(username: String): Response {
        val viewerId = securityContext.currentUserId
        val profile = profileReadService.getProfileByUsername(username, viewerId)

        return Response
            .ok(GetProfileByUsername200Response().profile(profile))
            .build()
    }

    @RolesAllowed("user")
    override fun followUserByUsername(username: String): Response {
        profileWriteService.followUser(username)

        val currentUserId = securityContext.currentUserId!!
        val profile = profileReadService.getProfileByUsername(username, currentUserId)
        return Response
            .ok(GetProfileByUsername200Response().profile(profile))
            .build()
    }

    @RolesAllowed("user")
    override fun unfollowUserByUsername(username: String): Response {
        profileWriteService.unfollowUser(username)

        val currentUserId = securityContext.currentUserId!!
        val profile = profileReadService.getProfileByUsername(username, currentUserId)
        return Response
            .ok(GetProfileByUsername200Response().profile(profile))
            .build()
    }
}
