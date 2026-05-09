package com.example.user

import com.example.common.security.PasswordHash
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@JvmInline
value class UserId(
    val value: Long,
)

data class User(
    val id: UserId,
    val email: String,
    val username: String,
    val passwordHash: PasswordHash,
    val bio: String? = null,
    val image: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

data class AuthenticatedUser(
    val email: String,
    val username: String,
    val bio: String,
    val image: String,
    val token: String,
    val refreshToken: String,
)

data class ProfileDto(
    val username: String,
    val bio: String,
    val image: String,
    val following: Boolean,
)

data class NewUserRequest(
    @field:Valid val user: NewUser,
)

data class NewUser(
    @field:NotBlank @field:Email @field:Size(max = 200) val email: String,
    @field:NotBlank @field:Size(max = 100) val username: String,
    @field:NotBlank val password: String,
)

data class LoginUserRequest(
    @field:Valid val user: LoginUser,
)

data class LoginUser(
    @field:NotBlank val email: String,
    @field:NotBlank val password: String,
)

data class UpdateUserRequest(
    @field:Valid val user: UpdateUser,
)

data class UpdateUser(
    @field:Email @field:Size(max = 200) val email: String?,
    @field:Size(max = 100) val username: String?,
    val password: String?,
    val bio: String?,
    val image: String?,
)

data class LogoutPayload(
    val refreshToken: String,
)

data class RefreshTokenPayload(
    val refreshToken: String,
)

data class UserEnvelope(
    val user: AuthenticatedUser,
)

data class ProfileEnvelope(
    val profile: ProfileDto,
)
