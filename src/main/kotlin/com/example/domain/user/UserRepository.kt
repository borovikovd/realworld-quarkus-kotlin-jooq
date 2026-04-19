package com.example.domain.user

import com.example.domain.shared.Repository

interface UserRepository : Repository<User, UserId> {
    fun findByEmail(email: String): User?

    fun findByUsername(username: String): User?

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean
}
