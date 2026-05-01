package com.example.application.readmodel

// refreshToken is "" (not null) for access-only responses (GET /user, PUT /user) because
// the OpenAPI spec declares the field as required. To fix: mark it optional in openapi.yaml,
// regenerate, and update callers. Not done — spec change affects client contract.
data class AuthenticatedUser(
    val user: UserReadModel,
    val accessToken: String,
    val refreshToken: String,
)
