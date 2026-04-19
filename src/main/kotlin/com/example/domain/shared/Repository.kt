package com.example.domain.shared

interface Repository<T : Entity<ID>, ID> {
    fun nextId(): ID

    fun create(entity: T): T

    fun update(entity: T): T

    fun findById(id: ID): T?
}
