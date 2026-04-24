package com.example.application.outport

import com.example.application.inport.query.readmodel.ProfileReadModel

interface ProfileReadRepository {
    fun findByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel?
}
