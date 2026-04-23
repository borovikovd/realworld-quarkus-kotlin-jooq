package com.example.application.query

import com.example.application.port.outbound.UserReadRepository
import com.example.application.query.readmodel.UserReadModel
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserQueries(
    private val userReadRepository: UserReadRepository,
) {
    fun getUserById(id: Long): UserReadModel? = userReadRepository.findById(id)
}
