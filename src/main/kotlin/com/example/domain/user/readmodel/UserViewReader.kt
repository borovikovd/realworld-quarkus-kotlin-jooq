package com.example.domain.user.readmodel

interface UserViewReader {
    fun getUserById(id: Long): UserView
}
