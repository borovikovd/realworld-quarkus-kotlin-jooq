package com.example.application.command

import com.example.application.port.inbound.command.FollowUserCommand
import com.example.application.port.inbound.command.UnfollowUserCommand
import com.example.application.port.outbound.CurrentUser
import com.example.application.port.outbound.FollowRepository
import com.example.application.port.outbound.UserWriteRepository
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
    fun followUser(command: FollowUserCommand) {
        val followerId = currentUser.require()
        val followee =
            userWriteRepository.findByUsername(Username(command.username))
                ?: throw NotFoundException("User not found")

        if (followee.id == followerId) {
            throw BadRequestException("Cannot follow yourself")
        }

        followRepository.follow(followerId, followee.id)
    }

    @Transactional
    fun unfollowUser(command: UnfollowUserCommand) {
        val followerId = currentUser.require()
        val followee =
            userWriteRepository.findByUsername(Username(command.username))
                ?: throw NotFoundException("User not found")

        followRepository.unfollow(followerId, followee.id)
    }
}
