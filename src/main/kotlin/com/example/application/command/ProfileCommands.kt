package com.example.application.command

import com.example.application.outport.CurrentUser
import com.example.application.outport.FollowRepository
import com.example.application.outport.UserWriteRepository
import com.example.domain.aggregate.user.Username
import com.example.domain.exception.BadRequestException
import com.example.domain.exception.NotFoundException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class ProfileCommands(
    private val userWriteRepository: UserWriteRepository,
    private val followRepository: FollowRepository,
    private val currentUser: CurrentUser,
) {
    @Transactional
    fun followUser(username: String) {
        val followerId = currentUser.require()
        val followee =
            userWriteRepository.findByUsername(Username(username))
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
            userWriteRepository.findByUsername(Username(username))
                ?: throw NotFoundException("User not found")

        followRepository.unfollow(followerId, followee.id)
    }
}
