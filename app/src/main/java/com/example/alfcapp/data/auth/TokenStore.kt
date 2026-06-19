package com.example.alfcapp.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.alfcapp.AlfcApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

object TokenStore {
    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_USERNAME = stringPreferencesKey("username")

    private val store get() = AlfcApplication.appContext.authDataStore

    val session: Flow<Session?> = store.data.map { prefs ->
        val token = prefs[KEY_ACCESS_TOKEN] ?: return@map null
        Session(
            accessToken = token,
            userId = prefs[KEY_USER_ID]?.toLongOrNull() ?: 0L,
            username = prefs[KEY_USERNAME].orEmpty(),
        )
    }

    suspend fun current(): Session? = session.first()

    /** Synchronous read used by the OkHttp interceptor — runs blockingly on the network thread. */
    fun currentBlocking(): Session? = kotlinx.coroutines.runBlocking { current() }

    suspend fun save(session: Session) {
        store.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = session.accessToken
            prefs[KEY_USER_ID] = session.userId.toString()
            prefs[KEY_USERNAME] = session.username
        }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }
}

data class Session(
    val accessToken: String,
    val userId: Long,
    val username: String
)
