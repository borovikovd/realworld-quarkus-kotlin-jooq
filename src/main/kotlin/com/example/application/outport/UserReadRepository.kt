package com.example.application.outport

import com.example.application.readmodel.UserReadModel

interface UserReadRepository {
    fun findById(id: Long): UserReadModel?
}
