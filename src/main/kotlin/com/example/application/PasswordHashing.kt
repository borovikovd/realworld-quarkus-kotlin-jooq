package com.example.application

import com.example.domain.aggregate.user.PasswordHash

interface PasswordHashing {
    fun hash(raw: String): PasswordHash

    fun verify(
        hash: PasswordHash,
        raw: String,
    ): Boolean
}
