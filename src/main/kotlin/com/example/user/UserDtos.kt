package com.example.user

import com.example.common.web.Patch
import com.example.common.web.StringPatchDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AuthenticatedUser(
    val email: String,
    val username: String,
    val bio: String?,
    val image: String?,
    val token: String,
    val refreshToken: String,
)

data class ProfileDto(
    val username: String,
    val bio: String?,
    val image: String?,
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
    @field:Email @field:Size(max = 200) val email: String? = null,
    @field:Size(max = 100) val username: String? = null,
    val password: String? = null,
    @field:JsonDeserialize(using = StringPatchDeserializer::class) val bio: Patch<String> = Patch.Absent,
    @field:JsonDeserialize(using = StringPatchDeserializer::class) val image: Patch<String> = Patch.Absent,
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
