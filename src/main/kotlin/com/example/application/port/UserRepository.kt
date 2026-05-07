package com.example.application.port

import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username

interface UserRepository : Repository<User, UserId> {
    fun existsByEmail(email: Email): Boolean

    fun existsByUsername(username: Username): Boolean

    fun erase(id: UserId)
}
