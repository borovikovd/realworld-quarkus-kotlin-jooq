package com.example.application.usecase.user

import com.example.application.readmodel.AuthenticatedUser

interface UserQueries {
    fun getUserById(id: Long): AuthenticatedUser?
}
