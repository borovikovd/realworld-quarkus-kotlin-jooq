package com.example.application.port.outbound

import com.example.application.port.outbound.ProfileReadModel

interface ProfileReadRepository {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel?
}
