package com.example.alfcapp.data.chat

import android.util.Log
import com.example.alfcapp.BuildConfig
import com.example.alfcapp.data.auth.TokenStore
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import java.util.concurrent.TimeUnit

/**
 * Singleton STOMP-over-WebSocket client for real-time chat.
 *
 * Connects to {@code ws[s]://<backend>/ws} as the currently signed-in member
 * and exposes a hot SharedFlow of {@link ChatEvent} so UI screens can react
 * without polling.
 *
 * Lifecycle:
 *  - {@link #start} is idempotent. Call once at app boot (see HomeScreen).
 *  - Whenever a new session appears in {@link TokenStore}, a fresh connection
 *    is established; when the session is cleared, the connection is dropped.
 *  - Disconnects retry with exponential backoff up to 30 s.
 */
object ChatWebSocket {

    private const val TAG = "ChatWebSocket"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            // collectLatest cancels the previous runLoop when the session changes —
            // i.e. on login / logout / token refresh.
            TokenStore.session.collectLatest { session ->
                if (session == null) {
                    _connected.value = false
                    return@collectLatest
                }
                runLoop(session.accessToken)
            }
        }
    }

    private suspend fun runLoop(jwt: String) {
        var backoff = 1_000L
        while (true) {
            try {
                connectOnce(jwt)
                // Clean disconnect (server closed) — try again right away.
                backoff = 1_000L
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                Log.w(TAG, "WS error: ${t.message}; retrying in ${backoff}ms")
                _connected.value = false
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(30_000L)
            }
        }
    }

    private suspend fun connectOnce(jwt: String) {
        val wsUrl = wsBaseUrl() + "ws"

        val okhttp = OkHttpClient.Builder()
            // Keeps the connection alive across mobile NATs.
            .pingInterval(20, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            // Long-lived; no read timeout.
            .readTimeout(0, TimeUnit.SECONDS)
            .build()

        val client = StompClient(OkHttpWebSocketClient(okhttp))

        val session: StompSession = client.connect(
            url = wsUrl,
            customStompConnectHeaders = mapOf("Authorization" to "Bearer $jwt"),
        )
        _connected.value = true
        Log.i(TAG, "STOMP connected to $wsUrl")

        try {
            val messages = session.subscribeText("/user/queue/messages")
            messages.collect { json ->
                try {
                    val ev = gson.fromJson(json, ChatEvent::class.java) ?: return@collect
                    _events.tryEmit(ev)
                } catch (t: CancellationException) {
                    throw t
                } catch (t: Throwable) {
                    Log.w(TAG, "Bad frame: ${t.message}")
                }
            }
        } finally {
            _connected.value = false
            try { session.disconnect() } catch (_: Throwable) { /* ignore */ }
            Log.i(TAG, "STOMP disconnected")
        }
    }

    /** Convert the REST base (http://host:8080/) to a WebSocket base (ws://host:8080/). */
    private fun wsBaseUrl(): String {
        val base = BuildConfig.BACKEND_BASE_URL
        return when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
            else -> base
        }.let { if (it.endsWith("/")) it else "$it/" }
    }
}
