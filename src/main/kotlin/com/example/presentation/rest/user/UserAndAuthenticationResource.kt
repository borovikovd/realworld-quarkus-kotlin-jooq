package com.example.presentation.rest.user

import com.example.api.UserAndAuthenticationApi
import com.example.api.model.CreateUserRequest
import com.example.api.model.Login200Response
import com.example.api.model.LoginRequest
import com.example.api.model.UpdateCurrentUserRequest
import com.example.application.CurrentUser
import com.example.application.UserService
import com.example.domain.auth.TokenIssuer
import com.example.domain.user.readmodel.UserView
import com.example.domain.user.readmodel.UserViewReader
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.resteasy.reactive.ResponseStatus
import com.example.api.model.User as ApiUser

@ApplicationScoped
class UserAndAuthenticationResource(
    private val userService: UserService,
    private val userViewReader: UserViewReader,
    private val tokenIssuer: TokenIssuer,
    private val currentUser: CurrentUser,
) : UserAndAuthenticationApi {
    @ResponseStatus(201)
    override fun createUser(body: CreateUserRequest): Login200Response {
        val newUser = body.user
        val userId =
            userService.register(
                email = newUser.email,
                username = newUser.username,
                password = newUser.password,
            )
        return Login200Response().user(userViewReader.getUserById(userId).toDto())
    }

    override fun login(body: LoginRequest): Login200Response {
        val loginUser = body.user
        val userId =
            userService.login(
                email = loginUser.email,
                password = loginUser.password,
            )
        return Login200Response().user(userViewReader.getUserById(userId).toDto())
    }

    @RolesAllowed("user")
    override fun getCurrentUser(): Login200Response {
        val userId = currentUser.require().value
        return Login200Response().user(userViewReader.getUserById(userId).toDto())
    }

    @RolesAllowed("user")
    override fun updateCurrentUser(body: UpdateCurrentUserRequest): Login200Response {
        val currentUserId = currentUser.require().value
        val updateUser = body.user

        val userId =
            userService.updateUser(
                userId = currentUserId,
                email = updateUser.email,
                username = updateUser.username,
                password = updateUser.password,
                bio = updateUser.bio,
                image = updateUser.image,
            )
        return Login200Response().user(userViewReader.getUserById(userId).toDto())
    }

    private fun UserView.toDto(): ApiUser =
        ApiUser()
            .email(email.value)
            .token(tokenIssuer.issue(id, email, username))
            .username(username.value)
            .bio(bio)
            .image(image)
}
