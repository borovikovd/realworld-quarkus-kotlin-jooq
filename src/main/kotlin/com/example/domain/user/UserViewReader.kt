package com.example.domain.user

import com.example.domain.user.readmodel.UserView

interface UserViewReader {
    fun getUserById(id: Long): UserView
}
