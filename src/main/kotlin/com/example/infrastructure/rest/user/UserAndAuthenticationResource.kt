package com.example.infrastructure.rest.user

import com.example.api.UserAndAuthenticationApi
import com.example.api.model.CreateUserRequest
import com.example.api.model.Login200Response
import com.example.api.model.LoginRequest
import com.example.api.model.RefreshTokenPayload
import com.example.api.model.UpdateCurrentUserRequest
import com.example.application.inport.command.UserCommands
import com.example.application.inport.query.UserQueries
import com.example.application.outport.CurrentUser
import com.example.application.outport.TokenIssuer
import com.example.application.readmodel.UserReadModel
import com.example.domain.aggregate.user.UserId
import com.example.domain.exception.NotFoundException
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.resteasy.reactive.ResponseStatus
import com.example.api.model.User as ApiUser

@ApplicationScoped
class UserAndAuthenticationResource(
    private val userCommands: UserCommands,
    private val userQueries: UserQueries,
    private val tokenIssuer: TokenIssuer,
    private val currentUser: CurrentUser,
) : UserAndAuthenticationApi {
    @ResponseStatus(201)
    override fun createUser(body: CreateUserRequest): Login200Response {
        val newUser = body.user
        val userId = userCommands.register(newUser.email, newUser.username, newUser.password)
        return Login200Response().user(loadUserWithFreshTokens(userId))
    }

    override fun login(body: LoginRequest): Login200Response {
        val loginUser = body.user
        val userId = userCommands.login(loginUser.email, loginUser.password)
        return Login200Response().user(loadUserWithFreshTokens(userId))
    }

    override fun refreshToken(body: RefreshTokenPayload): Login200Response {
        val result = userCommands.refresh(body.refreshToken)
        val user = userQueries.getUserById(result.userId) ?: throw NotFoundException("User not found")
        return Login200Response().user(user.toDto(result.tokens.accessToken, result.tokens.refreshToken))
    }

    @ResponseStatus(204)
    override fun logout(body: RefreshTokenPayload) {
        userCommands.logout(body.refreshToken)
    }

    @RolesAllowed("user")
    override fun getCurrentUser(): Login200Response {
        val userId = currentUser.require().value
        return Login200Response().user(loadUserAccessOnly(userId))
    }

    @RolesAllowed("user")
    override fun updateCurrentUser(body: UpdateCurrentUserRequest): Login200Response {
        val updateUser = body.user
        val userId =
            userCommands.updateUser(
                userId = currentUser.require().value,
                email = updateUser.email,
                username = updateUser.username,
                password = updateUser.password,
                bio = updateUser.bio,
                image = updateUser.image,
            )
        return Login200Response().user(loadUserAccessOnly(userId))
    }

    @RolesAllowed("user")
    @ResponseStatus(204)
    override fun deleteCurrentUser() {
        userCommands.eraseUser(currentUser.require().value)
    }

    private fun loadUserWithFreshTokens(userId: Long): ApiUser {
        val user = userQueries.getUserById(userId) ?: throw NotFoundException("User not found")
        val tokens = tokenIssuer.issue(UserId(userId))
        return user.toDto(tokens.accessToken, tokens.refreshToken)
    }

    private fun loadUserAccessOnly(userId: Long): ApiUser {
        val user = userQueries.getUserById(userId) ?: throw NotFoundException("User not found")
        // refreshToken is required by the schema; clients holding a still-valid refresh token
        // must keep using it. We return an empty placeholder so the contract holds without
        // minting a new refresh token (and DB row) on every authenticated request.
        return user.toDto(tokenIssuer.issueAccessToken(UserId(userId)), refreshToken = "")
    }

    private fun UserReadModel.toDto(
        accessToken: String,
        refreshToken: String,
    ): ApiUser =
        ApiUser()
            .email(email.value)
            .token(accessToken)
            .refreshToken(refreshToken)
            .username(username.value)
            .bio(bio)
            .image(image)
}
