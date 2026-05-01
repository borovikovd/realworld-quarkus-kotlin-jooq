package com.example.application.usecase

import com.example.application.readmodel.AuthenticatedUser

interface UserQueries {
    fun getUserById(id: Long): AuthenticatedUser?
}
