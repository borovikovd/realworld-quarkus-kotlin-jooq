package com.example.application.query

import com.example.application.port.inbound.query.GetProfileByUsernameQuery
import com.example.application.port.outbound.ProfileReadModel

interface ProfileQueries {
    fun getProfileByUsername(query: GetProfileByUsernameQuery): ProfileReadModel?
}
