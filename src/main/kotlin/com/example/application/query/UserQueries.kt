package com.example.application.query

import com.example.application.port.inbound.query.GetUserByIdQuery
import com.example.application.port.outbound.UserReadModel

interface UserQueries {
    fun getUserById(query: GetUserByIdQuery): UserReadModel?
}
