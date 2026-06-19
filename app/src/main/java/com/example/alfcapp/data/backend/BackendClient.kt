package com.example.alfcapp.data.backend

import com.example.alfcapp.BuildConfig
import com.example.alfcapp.data.auth.AuthApi
import com.example.alfcapp.data.auth.AuthInterceptor
import com.example.alfcapp.data.auth.MeApi
import com.example.alfcapp.data.chat.ChatApi
import com.example.alfcapp.data.events.EventApi
import com.example.alfcapp.data.livestream.LiveStreamApi
import com.example.alfcapp.data.prayer.PrayerApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object BackendClient {

    private val logging by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
    }

    private val unauthedHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val authedHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val unauthedRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(unauthedHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val authedRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(authedHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val eventApi: EventApi by lazy { unauthedRetrofit.create(EventApi::class.java) }
    val liveStreamApi: LiveStreamApi by lazy { unauthedRetrofit.create(LiveStreamApi::class.java) }

    val authApi: AuthApi by lazy { unauthedRetrofit.create(AuthApi::class.java) }
    val chatApi: ChatApi by lazy { authedRetrofit.create(ChatApi::class.java) }
    val meApi: MeApi by lazy { authedRetrofit.create(MeApi::class.java) }

    // Authed client because AuthInterceptor adds the JWT when present; if the
    // user isn't signed in, the interceptor leaves the request untouched and
    // the server falls back to the X-Device-Id header for anonymity.
    val prayerApi: PrayerApi by lazy { authedRetrofit.create(PrayerApi::class.java) }
}
