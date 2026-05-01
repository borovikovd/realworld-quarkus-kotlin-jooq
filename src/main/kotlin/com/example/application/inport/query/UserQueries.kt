package com.example.application.inport.query

import com.example.application.readmodel.AuthenticatedUser

interface UserQueries {
    fun getUserById(id: Long): AuthenticatedUser?
}
