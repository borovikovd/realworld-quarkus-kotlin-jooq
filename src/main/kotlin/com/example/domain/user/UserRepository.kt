package com.example.domain.user

import com.example.domain.shared.Repository

interface UserRepository : Repository<User, UserId> {
    fun findByEmail(email: Email): User?

    fun findByUsername(username: Username): User?

    fun existsByEmail(email: Email): Boolean

    fun existsByUsername(username: Username): Boolean
}
