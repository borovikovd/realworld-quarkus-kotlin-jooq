package com.example.user

import com.example.common.security.CurrentUser
import com.example.common.web.NotFoundException
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.jboss.resteasy.reactive.ResponseStatus

@ApplicationScoped
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class UserResource(
    private val userService: UserService,
    private val currentUser: CurrentUser,
) {
    @POST
    @Path("/users")
    @ResponseStatus(201)
    @APIResponse(responseCode = "201", description = "Created")
    fun createUser(
        @Valid body: NewUserRequest,
    ): UserEnvelope {
        val u = body.user
        return UserEnvelope(userService.register(u.email, u.username, u.password))
    }

    @POST
    @Path("/users/login")
    fun login(
        @Valid body: LoginUserRequest,
    ): UserEnvelope {
        val u = body.user
        return UserEnvelope(userService.login(u.email, u.password))
    }

    @POST
    @Path("/users/refresh")
    fun refreshToken(body: RefreshTokenPayload): UserEnvelope = UserEnvelope(userService.refresh(body.refreshToken))

    @POST
    @Path("/users/logout")
    @RolesAllowed("user")
    @ResponseStatus(204)
    @APIResponse(responseCode = "204", description = "No Content")
    fun logout(body: LogoutPayload) {
        userService.logout(body.refreshToken, currentUser.jti, currentUser.id)
    }

    @GET
    @Path("/user")
    @RolesAllowed("user")
    fun getCurrentUser(): UserEnvelope {
        val userId = currentUser.require()
        val rawToken = currentUser.rawToken ?: throw NotFoundException("token", "Token not present")
        return UserEnvelope(userService.getCurrentUser(userId, rawToken))
    }

    @PUT
    @Path("/user")
    @RolesAllowed("user")
    fun updateCurrentUser(
        @Valid body: UpdateUserRequest,
    ): UserEnvelope {
        val u = body.user
        return UserEnvelope(
            userService.updateUser(
                userId = currentUser.require(),
                email = u.email,
                username = u.username,
                password = u.password,
                bio = u.bio,
                image = u.image,
            ),
        )
    }

    @DELETE
    @Path("/user")
    @RolesAllowed("user")
    @ResponseStatus(204)
    fun deleteCurrentUser() {
        userService.eraseUser(currentUser.require(), currentUser.jti)
    }
}
