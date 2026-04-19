package com.example.application.user

interface UserViewReader {
    fun hydrate(id: Long): UserView
}
