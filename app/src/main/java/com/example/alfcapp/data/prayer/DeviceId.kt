package com.example.alfcapp.data.prayer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.alfcapp.AlfcApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.UUID

private val Context.deviceDataStore by preferencesDataStore(name = "device_prefs")

/**
 * Stable per-install identifier the app sends with anonymous prayer requests.
 * Used server-side for rate limiting, idempotent "I prayed" taps, and
 * letting the original submitter delete their own post — never exposed
 * to other users.
 */
object DeviceId {
    private val KEY = stringPreferencesKey("device_id")

    private val store get() = AlfcApplication.appContext.deviceDataStore

    suspend fun get(): String {
        val existing = store.data.map { it[KEY] }.first()
        if (existing != null) return existing
        val generated = UUID.randomUUID().toString()
        store.edit { it[KEY] = generated }
        return generated
    }

    /** Synchronous read for code paths that can't suspend. */
    fun getBlocking(): String = runBlocking { get() }
}
