package com.example.domain.auth

import com.example.user.domain.PasswordHash

interface PasswordHashing {
    fun hash(raw: String): PasswordHash

    fun verify(
        hash: PasswordHash,
        raw: String,
    ): Boolean
}
