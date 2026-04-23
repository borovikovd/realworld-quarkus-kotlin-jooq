package com.example.application.port.outbound

import com.example.application.query.readmodel.UserReadModel

interface UserReadRepository {
    fun findById(id: Long): UserReadModel?
}
