package com.example.user

import com.example.shared.domain.Repository

interface UserRepository : Repository<User, UserId> {
    fun findByEmail(email: String): User?

    fun findByUsername(username: String): User?

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean
}
