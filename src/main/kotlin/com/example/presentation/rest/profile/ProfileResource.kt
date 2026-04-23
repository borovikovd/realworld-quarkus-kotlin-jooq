package com.example.presentation.rest.profile

import com.example.api.ProfileApi
import com.example.api.model.GetProfileByUsername200Response
import com.example.api.model.Profile
import com.example.application.command.ProfileCommands
import com.example.application.port.inbound.command.FollowUserCommand
import com.example.application.port.inbound.command.UnfollowUserCommand
import com.example.application.port.inbound.query.GetProfileByUsernameQuery
import com.example.application.port.outbound.CurrentUser
import com.example.application.port.outbound.ProfileReadModel
import com.example.application.query.ProfileQueries
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
        profileCommands.followUser(FollowUserCommand(username))
        return GetProfileByUsername200Response().profile(requireProfile(username, currentUser.require().value))
    }

    @RolesAllowed("user")
    override fun unfollowUserByUsername(username: String): GetProfileByUsername200Response {
        profileCommands.unfollowUser(UnfollowUserCommand(username))
        return GetProfileByUsername200Response().profile(requireProfile(username, currentUser.require().value))
    }

    private fun requireProfile(
        username: String,
        viewerId: Long?,
    ): Profile =
        (
            profileQueries.getProfileByUsername(GetProfileByUsernameQuery(username, viewerId))
                ?: throw NotFoundException("Profile not found")
        ).toDto()
}

private fun ProfileReadModel.toDto(): Profile =
    Profile()
        .username(username)
        .bio(bio)
        .image(image)
        .following(following)
