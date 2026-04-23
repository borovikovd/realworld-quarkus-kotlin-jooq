package com.example.domain.user

import com.example.domain.Repository
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username

interface UserRepository : Repository<User, UserId> {
    fun findByEmail(email: Email): User?

    fun findByUsername(username: Username): User?

    fun existsByEmail(email: Email): Boolean

    fun existsByUsername(username: Username): Boolean
}
