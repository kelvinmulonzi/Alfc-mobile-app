package com.example.alfcapp.data.livestream

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface LiveStreamApi {
    @GET("api/live/status")
    suspend fun getLiveStatus(): LiveStreamDto

    /**
     * Admin-only: forces the backend to re-check YouTube immediately.
     * We also use this to validate the admin token from the broadcaster screen —
     * a 2xx means the X-Admin-Token matched `app.admin-token` on the server.
     */
    @POST("api/admin/live/refresh")
    suspend fun adminRefresh(@Header("X-Admin-Token") adminToken: String): LiveStreamDto
}
