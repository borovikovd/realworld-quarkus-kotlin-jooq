package com.example.profile.application

import com.example.profile.domain.FollowRepository
import com.example.shared.architecture.WriteService
import com.example.shared.exceptions.BadRequestException
import com.example.shared.exceptions.NotFoundException
import com.example.shared.security.SecurityContext
import com.example.user.domain.UserRepository
import jakarta.transaction.Transactional

@WriteService
class DefaultProfileWriteService(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val securityContext: SecurityContext,
) : ProfileWriteService {
    @Transactional
    override fun followUser(username: String) {
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
    override fun unfollowUser(username: String) {
        val followerId = securityContext.requireCurrentUserId()
        val followee =
            userRepository.findByUsername(username)
                ?: throw NotFoundException("User not found")

        followRepository.unfollow(followerId, followee.id)
    }
}
