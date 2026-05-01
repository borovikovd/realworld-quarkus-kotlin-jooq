package com.example.application.port.persistence

import com.example.domain.Entity

interface Repository<T : Entity<ID>, ID> {
    fun nextId(): ID

    fun create(entity: T): T

    fun update(entity: T): T

    fun findById(id: ID): T?
}
