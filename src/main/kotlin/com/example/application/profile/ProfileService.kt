package com.example.application.profile

import com.example.application.CurrentUser
import com.example.domain.profile.FollowRepository
import com.example.domain.shared.BadRequestException
import com.example.domain.shared.NotFoundException
import com.example.domain.user.UserRepository
import com.example.domain.user.Username
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class ProfileService(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val currentUser: CurrentUser,
) {
    @Transactional
    fun followUser(username: String) {
        val followerId = currentUser.require()
        val followee =
            userRepository.findByUsername(Username(username))
                ?: throw NotFoundException("User not found")

        if (followee.id == followerId) {
            throw BadRequestException("Cannot follow yourself")
        }

        followRepository.follow(followerId, followee.id)
    }

    @Transactional
    fun unfollowUser(username: String) {
        val followerId = currentUser.require()
        val followee =
            userRepository.findByUsername(Username(username))
                ?: throw NotFoundException("User not found")

        followRepository.unfollow(followerId, followee.id)
    }
}
