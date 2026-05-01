package com.example.application.port

import java.time.OffsetDateTime

interface Clock {
    fun now(): OffsetDateTime
}
