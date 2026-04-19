package com.example.application.profile

import com.example.domain.profile.FollowRepository
import com.example.domain.shared.BadRequestException
import com.example.domain.shared.NotFoundException
import com.example.domain.user.UserRepository
import com.example.shared.security.SecurityContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
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
