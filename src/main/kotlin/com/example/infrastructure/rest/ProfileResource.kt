package com.example.infrastructure.rest

import com.example.api.ProfileApi
import com.example.api.model.GetProfileByUsername200Response
import com.example.api.model.Profile
import com.example.application.port.security.CurrentUser
import com.example.application.readmodel.ProfileReadModel
import com.example.application.usecase.ProfileCommands
import com.example.application.usecase.ProfileQueries
import com.example.domain.exception.NotFoundException
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ProfileResource(
    private val profileCommands: ProfileCommands,
    private val profileQueries: ProfileQueries,
    private val currentUser: CurrentUser,
) : ProfileApi {
    override fun getProfileByUsername(username: String): GetProfileByUsername200Response =
        GetProfileByUsername200Response().profile(requireProfile(username, currentUser.id?.value))

    @RolesAllowed("user")
    override fun followUserByUsername(username: String): GetProfileByUsername200Response {
        profileCommands.followUser(username)
        return GetProfileByUsername200Response().profile(requireProfile(username, currentUser.require().value))
    }

    @RolesAllowed("user")
    override fun unfollowUserByUsername(username: String): GetProfileByUsername200Response {
        profileCommands.unfollowUser(username)
        return GetProfileByUsername200Response().profile(requireProfile(username, currentUser.require().value))
    }

    private fun requireProfile(
        username: String,
        viewerId: Long?,
    ): Profile =
        (profileQueries.getProfileByUsername(username, viewerId) ?: throw NotFoundException("Profile not found"))
            .toDto()
}

private fun ProfileReadModel.toDto(): Profile =
    Profile()
        .username(username)
        .bio(bio)
        .image(image)
        .following(following)
