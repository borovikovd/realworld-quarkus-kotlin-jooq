package com.example.application.port.outbound

import java.time.OffsetDateTime

interface Clock {
    fun now(): OffsetDateTime
}
