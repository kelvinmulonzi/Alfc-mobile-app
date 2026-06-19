package com.example.alfcapp.data.auth

import retrofit2.http.GET

interface MeApi {
    @GET("api/me")
    suspend fun me(): MeDto
}

data class MeDto(
    val userId: Long,
    val username: String
)
