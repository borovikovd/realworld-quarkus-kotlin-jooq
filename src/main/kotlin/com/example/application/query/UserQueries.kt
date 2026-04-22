package com.example.application.query

import com.example.application.query.readmodel.UserReadModel

interface UserQueries {
    fun getUserById(id: Long): UserReadModel?
}
