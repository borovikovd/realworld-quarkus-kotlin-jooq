package com.example.application.inport.query

import com.example.application.inport.query.readmodel.UserReadModel

interface UserQueries {
    fun getUserById(id: Long): UserReadModel?
}
