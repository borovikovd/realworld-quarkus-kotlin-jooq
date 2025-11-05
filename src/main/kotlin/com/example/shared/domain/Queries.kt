package com.example.shared.domain

/**
 * Marker interface for query services (CQRS read side).
 *
 * Queries return read-optimized DTOs and may bypass domain entities.
 * They should not modify state and don't need transactions.
 */
interface Queries
