package com.example.domain.user

import com.example.domain.user.readmodel.UserView

interface UserViewReader {
    fun hydrate(id: Long): UserView
}
