package com.example.application.profile

import com.example.domain.profile.FollowRepository
import com.example.domain.shared.BadRequestException
import com.example.domain.shared.NotFoundException
import com.example.application.CurrentUser
import com.example.domain.user.User
import com.example.domain.user.UserId
import com.example.domain.user.UserRepository
import com.example.domain.user.Email
import com.example.domain.user.PasswordHash
import com.example.domain.user.Username
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class DefaultProfileWriteServiceTest {
    private lateinit var profileWriteService: DefaultProfileWriteService
    private lateinit var userRepository: UserRepository
    private lateinit var followRepository: FollowRepository
    private lateinit var currentUser: CurrentUser

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        followRepository = mockk()
        currentUser = mockk()
        profileWriteService = DefaultProfileWriteService(
            userRepository = userRepository,
            followRepository = followRepository,
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
        every { userRepository.findByUsername(Username(username)) } returns followee
        every { followRepository.follow(followerId, followee.id) } returns Unit

        profileWriteService.followUser(username)

        verify { currentUser.require() }
        verify { userRepository.findByUsername(Username(username)) }
        verify { followRepository.follow(followerId, followee.id) }
    }

    @Test
    fun `followUser should throw NotFoundException when user not found`() {
        val followerId = UserId(1L)
        val username = "nonexistent"

        every { currentUser.require() } returns followerId
        every { userRepository.findByUsername(Username(username)) } returns null

        val exception =
            assertThrows<NotFoundException> {
                profileWriteService.followUser(username)
            }

        assertEquals("User not found", exception.message)
        verify { currentUser.require() }
        verify { userRepository.findByUsername(Username(username)) }
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
        every { userRepository.findByUsername(Username(username)) } returns followee

        val exception =
            assertThrows<BadRequestException> {
                profileWriteService.followUser(username)
            }

        assertEquals("Cannot follow yourself", exception.message)
        verify { currentUser.require() }
        verify { userRepository.findByUsername(Username(username)) }
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
        every { userRepository.findByUsername(Username(username)) } returns followee
        every { followRepository.unfollow(followerId, followee.id) } returns Unit

        profileWriteService.unfollowUser(username)

        verify { currentUser.require() }
        verify { userRepository.findByUsername(Username(username)) }
        verify { followRepository.unfollow(followerId, followee.id) }
    }

    @Test
    fun `unfollowUser should throw NotFoundException when user not found`() {
        val followerId = UserId(1L)
        val username = "nonexistent"

        every { currentUser.require() } returns followerId
        every { userRepository.findByUsername(Username(username)) } returns null

        val exception =
            assertThrows<NotFoundException> {
                profileWriteService.unfollowUser(username)
            }

        assertEquals("User not found", exception.message)
        verify { currentUser.require() }
        verify { userRepository.findByUsername(Username(username)) }
        verify(exactly = 0) { followRepository.unfollow(any(), any()) }
    }
}
