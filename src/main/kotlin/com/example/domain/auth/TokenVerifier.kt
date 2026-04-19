package com.example.domain.auth

import com.example.domain.user.UserId

interface TokenVerifier {
    fun verify(token: String): UserId?
}
