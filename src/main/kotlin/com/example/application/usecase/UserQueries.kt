package com.example.application.usecase

import com.example.application.readmodel.UserReadModel
import com.example.domain.aggregate.user.UserId

interface UserQueries {
    fun getUserById(id: UserId): UserReadModel?
}
