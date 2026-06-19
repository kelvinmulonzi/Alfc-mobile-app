package com.example.alfcapp.data.prayer

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PrayerApi {

    @GET("api/prayers")
    suspend fun list(
        @Header("X-Device-Id") deviceId: String,
        @Query("category") category: PrayerCategory? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): List<PrayerDto>

    @POST("api/prayers")
    suspend fun create(
        @Header("X-Device-Id") deviceId: String,
        @Body body: PrayerCreateRequest,
    ): PrayerDto

    @POST("api/prayers/{id}/pray")
    suspend fun pray(
        @Path("id") id: Long,
        @Header("X-Device-Id") deviceId: String,
    ): PrayCountDto

    @DELETE("api/prayers/{id}")
    suspend fun delete(
        @Path("id") id: Long,
        @Header("X-Device-Id") deviceId: String,
    )
}
