package com.example.application.user

import com.example.application.user.readmodel.UserView

interface UserViewReader {
    fun hydrate(id: Long): UserView
}
