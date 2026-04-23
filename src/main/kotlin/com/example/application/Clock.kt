package com.example.application

import java.time.OffsetDateTime

interface Clock {
    fun now(): OffsetDateTime
}
