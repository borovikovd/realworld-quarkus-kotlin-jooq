package com.example.presentation.rest.user

import com.example.api.UserAndAuthenticationApi
import com.example.api.model.CreateUserRequest
import com.example.api.model.Login200Response
import com.example.api.model.LoginRequest
import com.example.api.model.UpdateCurrentUserRequest
import com.example.application.CurrentUser
import com.example.application.user.UserWriteService
import com.example.domain.user.UserViewReader
import com.example.domain.user.readmodel.UserView
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.resteasy.reactive.ResponseStatus
import com.example.api.model.User as ApiUser

@ApplicationScoped
class UserAndAuthenticationResource(
    private val userWriteService: UserWriteService,
    private val userViewReader: UserViewReader,
    private val currentUser: CurrentUser,
) : UserAndAuthenticationApi {
    @ResponseStatus(201)
    override fun createUser(body: CreateUserRequest): Login200Response {
        val newUser = body.user
        val userId =
            userWriteService.register(
                email = newUser.email,
                username = newUser.username,
                password = newUser.password,
            )

        val userDto = userViewReader.hydrate(userId).toDto()

        return Login200Response().user(userDto)
    }

    override fun login(body: LoginRequest): Login200Response {
        val loginUser = body.user
        val userId =
            userWriteService.login(
                email = loginUser.email,
                password = loginUser.password,
            )

        val userDto = userViewReader.hydrate(userId).toDto()

        return Login200Response().user(userDto)
    }

    @RolesAllowed("user")
    override fun getCurrentUser(): Login200Response {
        val userId = currentUser.require().value
        val userDto = userViewReader.hydrate(userId).toDto()

        return Login200Response().user(userDto)
    }

    @RolesAllowed("user")
    override fun updateCurrentUser(body: UpdateCurrentUserRequest): Login200Response {
        val currentUserId = currentUser.require().value
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

        val userDto = userViewReader.hydrate(userId).toDto()

        return Login200Response().user(userDto)
    }
}

private fun UserView.toDto(): ApiUser =
    ApiUser()
        .email(email)
        .token(token)
        .username(username)
        .bio(bio)
        .image(image)
