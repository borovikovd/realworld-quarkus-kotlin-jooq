package com.example.common.time

import jakarta.enterprise.context.ApplicationScoped
import java.time.OffsetDateTime

interface Clock {
    fun now(): OffsetDateTime
}

@ApplicationScoped
class SystemClock : Clock {
    override fun now(): OffsetDateTime = OffsetDateTime.now()
}
