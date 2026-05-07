package com.example.application.service

import com.example.application.port.FollowRepository
import com.example.application.port.ProfileFinder
import com.example.application.port.UserFinder
import com.example.application.port.security.CurrentUser
import com.example.application.readmodel.ProfileReadModel
import com.example.application.usecase.ProfileCommands
import com.example.application.usecase.ProfileQueries
import com.example.domain.aggregate.user.UserId
import com.example.domain.exception.NotFoundException
import com.example.domain.exception.ValidationException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class ProfileService(
    private val followRepository: FollowRepository,
    private val profileFinder: ProfileFinder,
    private val userFinder: UserFinder,
    private val currentUser: CurrentUser,
) : ProfileCommands,
    ProfileQueries {
    @Transactional
    override fun followUser(username: String) {
        val followerId = currentUser.require()
        val followeeId =
            userFinder.findUserIdByUsername(username)
                ?: throw NotFoundException("User not found")

        if (followeeId == followerId) {
            throw ValidationException(mapOf("username" to listOf("cannot follow yourself")))
        }

        followRepository.follow(followerId, followeeId)
    }

    @Transactional
    override fun unfollowUser(username: String) {
        val followerId = currentUser.require()
        val followeeId =
            userFinder.findUserIdByUsername(username)
                ?: throw NotFoundException("User not found")

        followRepository.unfollow(followerId, followeeId)
    }

    override fun getProfileByUsername(
        username: String,
        viewerId: UserId?,
    ): ProfileReadModel? = profileFinder.findByUsername(username, viewerId)
}
