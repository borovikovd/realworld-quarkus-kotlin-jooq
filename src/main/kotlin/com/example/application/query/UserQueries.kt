package com.example.application.query

import com.example.application.outport.UserReadRepository
import com.example.application.inport.query.readmodel.UserReadModel
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserQueries(
    private val userReadRepository: UserReadRepository,
) {
    fun getUserById(id: Long): UserReadModel? = userReadRepository.findById(id)
}
