package com.example.infrastructure.time

import com.example.shared.domain.Clock
import jakarta.enterprise.context.ApplicationScoped
import java.time.OffsetDateTime

@ApplicationScoped
class SystemClock : Clock {
    override fun now(): OffsetDateTime = OffsetDateTime.now()
}
