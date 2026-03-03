package com.example.user.application

import com.example.shared.architecture.WriteService
import com.example.shared.exceptions.UnauthorizedException
import com.example.shared.exceptions.ValidationException
import com.example.shared.security.PasswordHasher
import com.example.user.domain.User
import com.example.user.domain.UserId
import com.example.user.domain.UserRepository
import jakarta.transaction.Transactional

@WriteService
class DefaultUserWriteService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
) : UserWriteService {
    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }

    @Transactional
    override fun register(
        email: String,
        username: String,
        password: String,
    ): Long {
        val errors = mutableMapOf<String, List<String>>()

        validateEmailFormat(email, errors)
        if ("email" !in errors && userRepository.existsByEmail(email)) {
            errors["email"] = listOf("is already taken")
        }

        validateUsernameFormat(username, errors)
        if ("username" !in errors && userRepository.existsByUsername(username)) {
            errors["username"] = listOf("is already taken")
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            errors["password"] = listOf("must be at least $MIN_PASSWORD_LENGTH characters")
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        val userId = userRepository.nextId()
        val passwordHash = passwordHasher.hash(password)
        val user = User(id = userId, email = email, username = username, passwordHash = passwordHash)
        userRepository.create(user)
        return userId.value
    }

    override fun login(
        email: String,
        password: String,
    ): Long {
        val user =
            userRepository.findByEmail(email)
                ?: throw UnauthorizedException("Invalid email or password")

        if (!passwordHasher.verify(user.passwordHash, password)) {
            throw UnauthorizedException("Invalid email or password")
        }

        return user.id.value
    }

    @Transactional
    override fun updateUser(
        userId: Long,
        email: String?,
        username: String?,
        password: String?,
        bio: String?,
        image: String?,
    ): Long {
        val typedUserId = UserId(userId)
        val user =
            userRepository.findById(typedUserId)
                ?: throw UnauthorizedException("User not found")

        val errors = mutableMapOf<String, List<String>>()

        email?.let {
            validateEmailFormat(it, errors)
            if ("email" !in errors && it != user.email && userRepository.existsByEmail(it)) {
                errors["email"] = listOf("is already taken")
            }
        }

        username?.let {
            validateUsernameFormat(it, errors)
            if ("username" !in errors && it != user.username && userRepository.existsByUsername(it)) {
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
        return saved.id.value
    }

    private fun validateEmailFormat(
        email: String,
        errors: MutableMap<String, List<String>>,
    ) {
        if (email.isBlank()) {
            errors["email"] = listOf("must not be blank")
        } else if (!User.EMAIL_REGEX.matches(email)) {
            errors["email"] = listOf("must be a valid email address")
        }
    }

    private fun validateUsernameFormat(
        username: String,
        errors: MutableMap<String, List<String>>,
    ) {
        if (username.isBlank()) {
            errors["username"] = listOf("must not be blank")
        } else if (username.length !in User.MIN_USERNAME_LENGTH..User.MAX_USERNAME_LENGTH) {
            errors["username"] =
                listOf(
                    "must be between ${User.MIN_USERNAME_LENGTH} and ${User.MAX_USERNAME_LENGTH} characters",
                )
        }
    }
}
