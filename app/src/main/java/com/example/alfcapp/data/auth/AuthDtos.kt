package com.example.alfcapp.data.auth

data class RegisterRequest(val username: String, val password: String)

data class LoginRequest(val username: String, val password: String)

data class AuthResponse(
    val accessToken: String,
    val userId: Long,
    val username: String
)
