package com.example.infrastructure.rest.user

import com.example.api.UserAndAuthenticationApi
import com.example.api.model.CreateUserRequest
import com.example.api.model.Login200Response
import com.example.api.model.LoginRequest
import com.example.api.model.UpdateCurrentUserRequest
import com.example.application.command.UserCommands
import com.example.application.port.outbound.CurrentUser
import com.example.application.port.outbound.TokenIssuer
import com.example.application.query.UserQueries
import com.example.application.query.readmodel.UserReadModel
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
        return Login200Response().user(loadUser(userId))
    }

    override fun login(body: LoginRequest): Login200Response {
        val loginUser = body.user
        val userId = userCommands.login(loginUser.email, loginUser.password)
        return Login200Response().user(loadUser(userId))
    }

    @RolesAllowed("user")
    override fun getCurrentUser(): Login200Response = Login200Response().user(loadUser(currentUser.require().value))

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
        return Login200Response().user(loadUser(userId))
    }

    private fun loadUser(userId: Long): ApiUser {
        val user = userQueries.getUserById(userId) ?: throw NotFoundException("User not found")
        return user.toDto()
    }

    private fun UserReadModel.toDto(): ApiUser =
        ApiUser()
            .email(email.value)
            .token(tokenIssuer.issue(id, email, username))
            .username(username.value)
            .bio(bio)
            .image(image)
}
