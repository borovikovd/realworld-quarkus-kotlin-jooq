package com.example.application.query

import com.example.application.outport.ProfileReadRepository
import com.example.application.inport.query.readmodel.ProfileReadModel
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ProfileQueries(
    private val profileReadRepository: ProfileReadRepository,
) {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel? = profileReadRepository.findByUsername(username, viewerId)
}
