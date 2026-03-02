package com.example.user

import com.example.api.UserAndAuthenticationApi
import com.example.api.model.CreateUserRequest
import com.example.api.model.Login200Response
import com.example.api.model.LoginRequest
import com.example.api.model.UpdateCurrentUserRequest
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import com.example.api.model.User as ApiUser

@ApplicationScoped
class UserAndAuthenticationResource(
    private val userWriteService: UserWriteService,
    private val userReadService: UserReadService,
    private val securityContext: SecurityContext,
) : UserAndAuthenticationApi {
    override fun createUser(body: CreateUserRequest): Response {
        val newUser = body.user
        val userId =
            userWriteService.register(
                email = newUser.email,
                username = newUser.username,
                password = newUser.password,
            )

        val userDto = userReadService.hydrate(userId).toDto()

        return Response
            .status(Response.Status.CREATED)
            .entity(Login200Response().user(userDto))
            .build()
    }

    override fun login(body: LoginRequest): Response {
        val loginUser = body.user
        val userId =
            userWriteService.login(
                email = loginUser.email,
                password = loginUser.password,
            )

        val userDto = userReadService.hydrate(userId).toDto()

        return Response
            .ok(Login200Response().user(userDto))
            .build()
    }

    @RolesAllowed("user")
    override fun getCurrentUser(): Response {
        val userId = securityContext.requireCurrentUserId()
        val userDto = userReadService.hydrate(userId).toDto()

        return Response
            .ok(Login200Response().user(userDto))
            .build()
    }

    @RolesAllowed("user")
    override fun updateCurrentUser(body: UpdateCurrentUserRequest): Response {
        val currentUserId = securityContext.requireCurrentUserId()
        val updateUser = body.user

        val userId =
            userWriteService.updateUser(
                userId = currentUserId,
                email = updateUser.email,
                username = updateUser.username,
                password = updateUser.password,
                bio = updateUser.bio,
                image = updateUser.image,
            )

        val userDto = userReadService.hydrate(userId).toDto()

        return Response
            .ok(Login200Response().user(userDto))
            .build()
    }
}

private fun UserSummary.toDto(): ApiUser =
    ApiUser()
        .email(email)
        .token(token)
        .username(username)
        .bio(bio)
        .image(image)
