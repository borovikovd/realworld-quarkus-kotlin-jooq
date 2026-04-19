package com.example.domain.auth

import com.example.user.domain.UserId

interface TokenVerifier {
    fun verify(token: String): UserId?
}
