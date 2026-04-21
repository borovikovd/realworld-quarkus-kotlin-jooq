package com.example.presentation.rest.profile

import com.example.api.ProfileApi
import com.example.api.model.GetProfileByUsername200Response
import com.example.api.model.Profile
import com.example.application.CurrentUser
import com.example.application.profile.ProfileViewReader
import com.example.application.profile.ProfileWriteService
import com.example.application.profile.readmodel.ProfileView
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ProfileResource(
    private val profileWriteService: ProfileWriteService,
    private val profileViewReader: ProfileViewReader,
    private val currentUser: CurrentUser,
) : ProfileApi {
    override fun getProfileByUsername(username: String): GetProfileByUsername200Response {
        val viewerId = currentUser.id?.value
        val profile = profileViewReader.getProfileByUsername(username, viewerId).toDto()

        return GetProfileByUsername200Response().profile(profile)
    }

    @RolesAllowed("user")
    override fun followUserByUsername(username: String): GetProfileByUsername200Response {
        profileWriteService.followUser(username)

        val currentUserId = currentUser.require().value
        val profile = profileViewReader.getProfileByUsername(username, currentUserId).toDto()
        return GetProfileByUsername200Response().profile(profile)
    }

    @RolesAllowed("user")
    override fun unfollowUserByUsername(username: String): GetProfileByUsername200Response {
        profileWriteService.unfollowUser(username)

        val currentUserId = currentUser.require().value
        val profile = profileViewReader.getProfileByUsername(username, currentUserId).toDto()
        return GetProfileByUsername200Response().profile(profile)
    }
}

private fun ProfileView.toDto(): Profile =
    Profile()
        .username(username)
        .bio(bio)
        .image(image)
        .following(following)
