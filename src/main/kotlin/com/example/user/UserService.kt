package com.example.user

import com.example.shared.exceptions.UnauthorizedException
import com.example.shared.exceptions.ValidationException
import com.example.shared.security.PasswordHasher
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional

@ApplicationScoped
class UserService {
    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var passwordHasher: PasswordHasher

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }

    @Transactional
    fun register(
        email: String,
        username: String,
        password: String,
    ): UserId {
        val errors = mutableMapOf<String, List<String>>()

        if (userRepository.existsByEmail(email)) {
            errors["email"] = listOf("is already taken")
        }

        if (userRepository.existsByUsername(username)) {
            errors["username"] = listOf("is already taken")
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            errors["password"] = listOf("must be at least $MIN_PASSWORD_LENGTH characters")
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        val passwordHash = passwordHasher.hash(password)
        val user = User(email = email, username = username, passwordHash = passwordHash)
        val saved = userRepository.create(user)
        return UserId(saved.id!!)
    }

    fun login(
        email: String,
        password: String,
    ): UserId {
        val user =
            userRepository.findByEmail(email)
                ?: throw UnauthorizedException("Invalid email or password")

        if (!passwordHasher.verify(user.passwordHash, password)) {
            throw UnauthorizedException("Invalid email or password")
        }

        return UserId(user.id!!)
    }

    @Transactional
    fun updateUser(
        userId: Long,
        email: String?,
        username: String?,
        password: String?,
        bio: String?,
        image: String?,
    ): UserId {
        val user =
            userRepository.findById(userId)
                ?: throw UnauthorizedException("User not found")

        val errors = mutableMapOf<String, List<String>>()

        email?.let {
            if (it != user.email && userRepository.existsByEmail(it)) {
                errors["email"] = listOf("is already taken")
            }
        }

        username?.let {
            if (it != user.username && userRepository.existsByUsername(it)) {
                errors["username"] = listOf("is already taken")
            }
        }

        password?.let {
            if (it.length < MIN_PASSWORD_LENGTH) {
                errors["password"] = listOf("must be at least $MIN_PASSWORD_LENGTH characters")
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        var updatedUser = user.updateProfile(email, username, bio, image)

        password?.let {
            val newPasswordHash = passwordHasher.hash(it)
            updatedUser = updatedUser.updatePassword(newPasswordHash)
        }

        val saved = userRepository.update(updatedUser)
        return UserId(saved.id!!)
    }
}
