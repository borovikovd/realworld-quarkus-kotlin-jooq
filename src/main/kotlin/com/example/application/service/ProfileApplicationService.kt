package com.example.application.service

import com.example.application.inport.command.ProfileCommands
import com.example.application.inport.query.ProfileQueries
import com.example.application.outport.CurrentUser
import com.example.application.outport.ProfileRepository
import com.example.application.outport.UserRepository
import com.example.application.readmodel.ProfileReadModel
import com.example.domain.exception.BadRequestException
import com.example.domain.exception.NotFoundException
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
            throw BadRequestException("Cannot follow yourself")
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
