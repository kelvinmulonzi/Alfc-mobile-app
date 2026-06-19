package com.example.alfcapp.data.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = TokenStore.currentBlocking()?.accessToken
        val authed = if (token.isNullOrEmpty()) request
        else request.newBuilder().header("Authorization", "Bearer $token").build()
        return chain.proceed(authed)
    }
}
