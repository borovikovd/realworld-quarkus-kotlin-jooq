package com.example.domain.shared

import java.time.OffsetDateTime

interface Clock {
    fun now(): OffsetDateTime
}
