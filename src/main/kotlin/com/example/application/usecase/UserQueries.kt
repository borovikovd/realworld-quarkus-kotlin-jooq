package com.example.application.usecase

import com.example.application.readmodel.UserReadModel

interface UserQueries {
    fun getUserById(id: Long): UserReadModel?
}
