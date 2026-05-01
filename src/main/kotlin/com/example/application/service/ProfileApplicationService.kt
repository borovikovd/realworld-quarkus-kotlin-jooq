package com.example.application.service

import com.example.application.port.profile.ProfileRepository
import com.example.application.port.security.CurrentUser
import com.example.application.port.user.UserRepository
import com.example.application.readmodel.ProfileReadModel
import com.example.application.usecase.profile.ProfileCommands
import com.example.application.usecase.profile.ProfileQueries
import com.example.domain.exception.NotFoundException
import com.example.domain.exception.ValidationException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class ProfileApplicationService(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val currentUser: CurrentUser,
) : ProfileCommands,
    ProfileQueries {
    @Transactional
    override fun followUser(username: String) {
        val followerId = currentUser.require()
        val followeeId =
            userRepository.findUserIdByUsername(username)
                ?: throw NotFoundException("User not found")

        if (followeeId == followerId) {
            throw ValidationException(mapOf("username" to listOf("cannot follow yourself")))
        }

        profileRepository.follow(followerId, followeeId)
    }

    @Transactional
    override fun unfollowUser(username: String) {
        val followerId = currentUser.require()
        val followeeId =
            userRepository.findUserIdByUsername(username)
                ?: throw NotFoundException("User not found")

        profileRepository.unfollow(followerId, followeeId)
    }

    override fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel? = profileRepository.findByUsername(username, viewerId)
}
