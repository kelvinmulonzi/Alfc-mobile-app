package com.example.alfcapp.data.livestream

import com.example.alfcapp.data.backend.BackendClient

object LiveStreamRepository {

    suspend fun fetchStatus(): Result<LiveStreamDto> = runCatching {
        BackendClient.liveStreamApi.getLiveStatus()
    }

    /**
     * Validates the admin token by calling the admin-only refresh endpoint.
     * Returns success only when the backend accepts the token (HTTP 2xx).
     */
    suspend fun validateAdminToken(adminToken: String): Result<LiveStreamDto> = runCatching {
        BackendClient.liveStreamApi.adminRefresh(adminToken)
    }
}
