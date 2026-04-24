package com.example.application.command

import com.example.application.inport.command.ProfileCommands
import com.example.application.outport.CurrentUser
import com.example.application.outport.FollowRepository
import com.example.application.outport.ProfileReadRepository
import com.example.application.outport.UserWriteRepository
import com.example.application.service.ProfileApplicationService
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.PasswordHash
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.domain.exception.BadRequestException
import com.example.domain.exception.NotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ProfileCommandsTest {
    private lateinit var profileCommands: ProfileCommands
    private lateinit var userWriteRepository: UserWriteRepository
    private lateinit var followRepository: FollowRepository
    private lateinit var currentUser: CurrentUser

    @BeforeEach
    fun setup() {
        userWriteRepository = mockk()
        followRepository = mockk()
        currentUser = mockk()
        profileCommands = ProfileApplicationService(
            userWriteRepository = userWriteRepository,
            followRepository = followRepository,
            profileReadRepository = mockk(relaxed = true),
            currentUser = currentUser,
        )
    }

    @Test
    fun `followUser should follow when user exists`() {
        val followerId = UserId(1L)
        val username = "targetuser"

        val followee =
            User(
                id = UserId(2L),
                email = Email("target@example.com"),
                username = Username(username),
                passwordHash = PasswordHash("hash"),
            )

        every { currentUser.require() } returns followerId
        every { userWriteRepository.findByUsername(Username(username)) } returns followee
        every { followRepository.follow(followerId, followee.id) } returns Unit

        profileCommands.followUser(username)

        verify { currentUser.require() }
        verify { userWriteRepository.findByUsername(Username(username)) }
        verify { followRepository.follow(followerId, followee.id) }
    }

    @Test
    fun `followUser should throw NotFoundException when user not found`() {
        val followerId = UserId(1L)
        val username = "nonexistent"

        every { currentUser.require() } returns followerId
        every { userWriteRepository.findByUsername(Username(username)) } returns null

        val exception =
            assertThrows<NotFoundException> {
                profileCommands.followUser(username)
            }

        assertEquals("User not found", exception.message)
        verify { currentUser.require() }
        verify { userWriteRepository.findByUsername(Username(username)) }
        verify(exactly = 0) { followRepository.follow(any(), any()) }
    }

    @Test
    fun `followUser should throw BadRequestException when trying to follow self`() {
        val followerId = UserId(1L)
        val username = "selfuser"

        val followee =
            User(
                id = followerId,
                email = Email("self@example.com"),
                username = Username(username),
                passwordHash = PasswordHash("hash"),
            )

        every { currentUser.require() } returns followerId
        every { userWriteRepository.findByUsername(Username(username)) } returns followee

        val exception =
            assertThrows<BadRequestException> {
                profileCommands.followUser(username)
            }

        assertEquals("Cannot follow yourself", exception.message)
        verify { currentUser.require() }
        verify { userWriteRepository.findByUsername(Username(username)) }
        verify(exactly = 0) { followRepository.follow(any(), any()) }
    }

    @Test
    fun `unfollowUser should unfollow when user exists`() {
        val followerId = UserId(1L)
        val username = "targetuser"

        val followee =
            User(
                id = UserId(2L),
                email = Email("target@example.com"),
                username = Username(username),
                passwordHash = PasswordHash("hash"),
            )

        every { currentUser.require() } returns followerId
        every { userWriteRepository.findByUsername(Username(username)) } returns followee
        every { followRepository.unfollow(followerId, followee.id) } returns Unit

        profileCommands.unfollowUser(username)

        verify { currentUser.require() }
        verify { userWriteRepository.findByUsername(Username(username)) }
        verify { followRepository.unfollow(followerId, followee.id) }
    }

    @Test
    fun `unfollowUser should throw NotFoundException when user not found`() {
        val followerId = UserId(1L)
        val username = "nonexistent"

        every { currentUser.require() } returns followerId
        every { userWriteRepository.findByUsername(Username(username)) } returns null

        val exception =
            assertThrows<NotFoundException> {
                profileCommands.unfollowUser(username)
            }

        assertEquals("User not found", exception.message)
        verify { currentUser.require() }
        verify { userWriteRepository.findByUsername(Username(username)) }
        verify(exactly = 0) { followRepository.unfollow(any(), any()) }
    }
}
