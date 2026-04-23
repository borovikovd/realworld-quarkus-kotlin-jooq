package com.example.application.port.outbound

import com.example.application.port.outbound.UserReadModel

interface UserReadRepository {
    fun getUserById(id: Long): UserReadModel?
}
