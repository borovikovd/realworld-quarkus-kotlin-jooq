package com.example.profile.infrastructure

import com.example.api.ProfileApi
import com.example.api.model.GetProfileByUsername200Response
import com.example.api.model.Profile
import com.example.profile.application.ProfileReadService
import com.example.profile.application.ProfileSummary
import com.example.profile.application.ProfileWriteService
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ProfileResource(
    private val profileWriteService: ProfileWriteService,
    private val profileReadService: ProfileReadService,
    private val securityContext: SecurityContext,
) : ProfileApi {
    override fun getProfileByUsername(username: String): GetProfileByUsername200Response {
        val viewerId = securityContext.currentUserId?.value
        val profile = profileReadService.getProfileByUsername(username, viewerId).toDto()

        return GetProfileByUsername200Response().profile(profile)
    }

    @RolesAllowed("user")
    override fun followUserByUsername(username: String): GetProfileByUsername200Response {
        profileWriteService.followUser(username)

        val currentUserId = securityContext.currentUserId!!.value
        val profile = profileReadService.getProfileByUsername(username, currentUserId).toDto()
        return GetProfileByUsername200Response().profile(profile)
    }

    @RolesAllowed("user")
    override fun unfollowUserByUsername(username: String): GetProfileByUsername200Response {
        profileWriteService.unfollowUser(username)

        val currentUserId = securityContext.currentUserId!!.value
        val profile = profileReadService.getProfileByUsername(username, currentUserId).toDto()
        return GetProfileByUsername200Response().profile(profile)
    }
}

private fun ProfileSummary.toDto(): Profile =
    Profile()
        .username(username)
        .bio(bio)
        .image(image)
        .following(following)
