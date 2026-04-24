package com.example.application.outport

import java.time.OffsetDateTime

interface Clock {
    fun now(): OffsetDateTime
}
