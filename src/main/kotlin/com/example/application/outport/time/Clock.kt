package com.example.application.outport.time

import java.time.OffsetDateTime

interface Clock {
    fun now(): OffsetDateTime
}
