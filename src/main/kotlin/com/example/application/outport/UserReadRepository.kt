package com.example.application.outport

import com.example.application.inport.query.readmodel.UserReadModel

interface UserReadRepository {
    fun findById(id: Long): UserReadModel?
}
