package com.example.alfcapp.data.auth

import com.example.alfcapp.data.backend.BackendClient

object AuthRepository {
    suspend fun register(username: String, password: String): Session {
        val resp = BackendClient.authApi.register(RegisterRequest(username.trim(), password))
        return persist(resp)
    }

    suspend fun login(username: String, password: String): Session {
        val resp = BackendClient.authApi.login(LoginRequest(username.trim(), password))
        return persist(resp)
    }

    suspend fun signOut() {
        TokenStore.clear()
    }

    suspend fun fetchMe(): MeDto = BackendClient.meApi.me()

    private suspend fun persist(resp: AuthResponse): Session {
        val session = Session(
            accessToken = resp.accessToken,
            userId = resp.userId,
            username = resp.username
        )
        TokenStore.save(session)
        return session
    }
}
