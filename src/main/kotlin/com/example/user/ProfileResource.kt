package com.example.user

import com.example.common.security.CurrentUser
import com.example.common.web.NotFoundException
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@ApplicationScoped
@Path("/profiles/{username}")
@Produces(MediaType.APPLICATION_JSON)
class ProfileResource(
    private val userService: UserService,
    private val currentUser: CurrentUser,
) {
    @GET
    fun getProfileByUsername(
        @PathParam("username") username: String,
    ): ProfileEnvelope = ProfileEnvelope(requireProfile(username, currentUser.id))

    @POST
    @Path("/follow")
    @RolesAllowed("user")
    fun followUserByUsername(
        @PathParam("username") username: String,
    ): ProfileEnvelope {
        userService.followUser(username)
        return ProfileEnvelope(requireProfile(username, currentUser.require()))
    }

    @DELETE
    @Path("/follow")
    @RolesAllowed("user")
    fun unfollowUserByUsername(
        @PathParam("username") username: String,
    ): ProfileEnvelope {
        userService.unfollowUser(username)
        return ProfileEnvelope(requireProfile(username, currentUser.require()))
    }

    private fun requireProfile(
        username: String,
        viewerId: UserId?,
    ): ProfileDto =
        userService.getProfileByUsername(username, viewerId)
            ?: throw NotFoundException("profile", "Profile not found")
}
