package com.example.application.port

import com.example.application.port.Repository
import com.example.application.readmodel.LoginCredentials
import com.example.application.readmodel.UserReadModel
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username

interface UserRepository : Repository<User, UserId> {
    fun existsByEmail(email: Email): Boolean

    fun existsByUsername(username: Username): Boolean

    fun erase(id: UserId)

    fun findById(id: Long): UserReadModel?

    fun findCredentialsByEmail(email: Email): LoginCredentials?

    fun findUserIdByUsername(username: String): UserId?
}
