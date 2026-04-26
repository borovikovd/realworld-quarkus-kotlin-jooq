package com.example.application.service

import com.example.application.inport.command.ProfileCommands
import com.example.application.inport.query.ProfileQueries
import com.example.application.outport.CurrentUser
import com.example.application.outport.FollowWriteRepository
import com.example.application.outport.ProfileReadRepository
import com.example.application.outport.UserReadRepository
import com.example.application.readmodel.ProfileReadModel
import com.example.domain.exception.BadRequestException
import com.example.domain.exception.NotFoundException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class ProfileApplicationService(
    private val userReadRepository: UserReadRepository,
    private val followRepository: FollowWriteRepository,
    private val profileReadRepository: ProfileReadRepository,
    private val currentUser: CurrentUser,
) : ProfileCommands,
    ProfileQueries {
    @Transactional
    override fun followUser(username: String) {
        val followerId = currentUser.require()
        val followeeId =
            userReadRepository.findUserIdByUsername(username)
                ?: throw NotFoundException("User not found")

        if (followeeId == followerId) {
            throw BadRequestException("Cannot follow yourself")
        }

        followRepository.follow(followerId, followeeId)
    }

    @Transactional
    override fun unfollowUser(username: String) {
        val followerId = currentUser.require()
        val followeeId =
            userReadRepository.findUserIdByUsername(username)
                ?: throw NotFoundException("User not found")

        followRepository.unfollow(followerId, followeeId)
    }

    override fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel? = profileReadRepository.findByUsername(username, viewerId)
}
