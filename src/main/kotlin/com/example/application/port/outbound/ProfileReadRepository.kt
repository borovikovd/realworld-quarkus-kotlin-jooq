package com.example.application.port.outbound

import com.example.application.query.readmodel.ProfileReadModel

interface ProfileReadRepository {
    fun findByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel?
}
