package com.example.profile

import com.example.shared.architecture.WriteService
import com.example.shared.exceptions.BadRequestException
import com.example.shared.exceptions.NotFoundException
import com.example.shared.security.SecurityContext
import com.example.user.UserRepository
import jakarta.transaction.Transactional

@WriteService
class ProfileWriteService(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val securityContext: SecurityContext,
) {
    @Transactional
    fun followUser(username: String) {
        val followerId = securityContext.requireCurrentUserId()
        val followee =
            userRepository.findByUsername(username)
                ?: throw NotFoundException("User not found")

        if (followee.id == followerId) {
            throw BadRequestException("Cannot follow yourself")
        }

        followRepository.follow(followerId, followee.id)
    }

    @Transactional
    fun unfollowUser(username: String) {
        val followerId = securityContext.requireCurrentUserId()
        val followee =
            userRepository.findByUsername(username)
                ?: throw NotFoundException("User not found")

        followRepository.unfollow(followerId, followee.id)
    }
}
