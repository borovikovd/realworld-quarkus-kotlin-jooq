package com.example.application.service

import com.example.application.inport.command.ProfileCommands
import com.example.application.inport.query.ProfileQueries
import com.example.application.readmodel.ProfileReadModel
import com.example.application.outport.CurrentUser
import com.example.application.outport.FollowWriteRepository
import com.example.application.outport.ProfileReadRepository
import com.example.application.outport.UserWriteRepository
import com.example.domain.aggregate.user.Username
import com.example.domain.exception.BadRequestException
import com.example.domain.exception.NotFoundException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class ProfileApplicationService(
    private val userWriteRepository: UserWriteRepository,
    private val followRepository: FollowWriteRepository,
    private val profileReadRepository: ProfileReadRepository,
    private val currentUser: CurrentUser,
) : ProfileCommands,
    ProfileQueries {
    @Transactional
    override fun followUser(username: String) {
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
    override fun unfollowUser(username: String) {
        val followerId = currentUser.require()
        val followee =
            userWriteRepository.findByUsername(Username(username))
                ?: throw NotFoundException("User not found")

        followRepository.unfollow(followerId, followee.id)
    }

    override fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel? = profileReadRepository.findByUsername(username, viewerId)
}
