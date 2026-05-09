package com.example.user

import com.example.common.security.CurrentUser
import com.example.common.web.NotFoundException
import com.example.common.web.ValidationException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class ProfileService(
    private val userRepository: UserRepository,
    private val currentUser: CurrentUser,
) {
    @Transactional
    fun followUser(username: String) {
        val followerId = currentUser.require()
        val followeeId =
            userRepository.findUserIdByUsername(username)
                ?: throw NotFoundException("User not found")

        if (followeeId == followerId) {
            throw ValidationException(mapOf("username" to listOf("cannot follow yourself")))
        }

        userRepository.follow(followerId, followeeId)
    }

    @Transactional
    fun unfollowUser(username: String) {
        val followerId = currentUser.require()
        val followeeId =
            userRepository.findUserIdByUsername(username)
                ?: throw NotFoundException("User not found")

        userRepository.unfollow(followerId, followeeId)
    }

    fun getProfileByUsername(
        username: String,
        viewerId: UserId?,
    ): ProfileDto? = userRepository.findProfile(username, viewerId)
}
