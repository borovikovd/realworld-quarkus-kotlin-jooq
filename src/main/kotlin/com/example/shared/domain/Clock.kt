package com.example.shared.domain

import java.time.OffsetDateTime

interface Clock {
    fun now(): OffsetDateTime
}
