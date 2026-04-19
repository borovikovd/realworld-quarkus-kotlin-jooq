package com.example.domain.auth

import com.example.domain.user.PasswordHash

interface PasswordHashing {
    fun hash(raw: String): PasswordHash

    fun verify(
        hash: PasswordHash,
        raw: String,
    ): Boolean
}
