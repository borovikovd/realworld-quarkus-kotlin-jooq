package com.example.infrastructure.rest

import com.example.api.UserAndAuthenticationApi
import com.example.api.model.CreateUserRequest
import com.example.api.model.Login200Response
import com.example.api.model.LoginRequest
import com.example.api.model.LogoutPayload
import com.example.api.model.RefreshTokenPayload
import com.example.api.model.UpdateCurrentUserRequest
import com.example.application.port.security.CurrentUser
import com.example.application.readmodel.AuthenticatedUser
import com.example.application.readmodel.UserReadModel
import com.example.application.usecase.UserCommands
import com.example.application.usecase.UserQueries
import com.example.domain.exception.NotFoundException
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.resteasy.reactive.ResponseStatus
import com.example.api.model.User as ApiUser

@ApplicationScoped
class UserAndAuthenticationResource(
    private val userCommands: UserCommands,
    private val userQueries: UserQueries,
    private val currentUser: CurrentUser,
) : UserAndAuthenticationApi {
    @ResponseStatus(201)
    override fun createUser(body: CreateUserRequest): Login200Response {
        val newUser = body.user
        val result = userCommands.register(newUser.email, newUser.username, newUser.password)
        return Login200Response().user(result.toApiUser())
    }

    override fun login(body: LoginRequest): Login200Response {
        val loginUser = body.user
        return Login200Response().user(userCommands.login(loginUser.email, loginUser.password).toApiUser())
    }

    override fun refreshToken(body: RefreshTokenPayload): Login200Response =
        Login200Response().user(userCommands.refresh(body.refreshToken).toApiUser())

    @RolesAllowed("user")
    @ResponseStatus(204)
    override fun logout(body: LogoutPayload) {
        userCommands.logout(body.refreshToken, currentUser.jti, currentUser.id?.value)
    }

    @RolesAllowed("user")
    override fun getCurrentUser(): Login200Response {
        val userId = currentUser.require().value
        val user = userQueries.getUserById(userId) ?: throw NotFoundException("User not found")
        return Login200Response().user(user.toApiUser(accessToken = currentUser.rawToken ?: ""))
    }

    @RolesAllowed("user")
    override fun updateCurrentUser(body: UpdateCurrentUserRequest): Login200Response {
        val updateUser = body.user
        val result =
            userCommands.updateUser(
                userId = currentUser.require().value,
                email = updateUser.email,
                username = updateUser.username,
                password = updateUser.password,
                bio = updateUser.bio,
                image = updateUser.image,
            )
        return Login200Response().user(result.toApiUser())
    }

    @RolesAllowed("user")
    @ResponseStatus(204)
    override fun deleteCurrentUser() {
        userCommands.eraseUser(currentUser.require().value)
    }

    private fun AuthenticatedUser.toApiUser(): ApiUser =
        ApiUser()
            .email(user.email.value)
            .token(accessToken)
            .refreshToken(refreshToken)
            .username(user.username.value)
            .bio(user.bio)
            .image(user.image)

    private fun UserReadModel.toApiUser(accessToken: String): ApiUser =
        ApiUser()
            .email(email.value)
            .token(accessToken)
            .refreshToken("")
            .username(username.value)
            .bio(bio)
            .image(image)
}
