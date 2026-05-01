package com.example.application.port.time

import java.time.OffsetDateTime

interface Clock {
    fun now(): OffsetDateTime
}
