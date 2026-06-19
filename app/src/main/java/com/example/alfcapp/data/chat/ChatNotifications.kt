package com.example.alfcapp.data.chat

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.alfcapp.AlfcApplication
import com.example.alfcapp.MainActivity
import com.example.alfcapp.R
import com.example.alfcapp.data.auth.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Slow backstop poll for the unread badge. The WebSocket below pushes
 * `refreshNow()` on every incoming message, so the badge usually updates
 * in real time; this fallback only catches missed pushes during connection
 * gaps (network drop, app backgrounded, etc.).
 */
private const val POLL_INTERVAL_MS = 60_000L

/**
 * Process-wide unread-chat badge. Two things keep it fresh:
 *
 *  1. ChatWebSocket: real-time push events (instant badge refresh).
 *  2. A slow 60s poll: catches gaps when the socket is disconnected.
 *
 * Started lazily on first observation; pauses when the user signs out.
 */
object ChatNotifications {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _unreadThreads = MutableStateFlow(0)
    val unreadThreads: StateFlow<Int> = _unreadThreads.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true

        // Boot the WebSocket so we get real-time pushes.
        ChatWebSocket.start()

        // Any push event (new/deleted message or read receipt) can change
        // the unread count — refresh as soon as one arrives.
        scope.launch {
            ChatWebSocket.events.collect { ev ->
                refreshNow()
                if (ev.kind == ChatEvent.MESSAGE_NEW) {
                    maybePostMessageNotification(ev)
                }
            }
        }

        // Slow poll backstop while signed in.
        scope.launch {
            TokenStore.session.collectLatest { session ->
                if (session == null) {
                    _unreadThreads.value = 0
                    return@collectLatest
                }
                while (isActive) {
                    try {
                        _unreadThreads.value = ChatRepository.threads().count { it.unread }
                    } catch (_: Throwable) { /* keep last value */ }
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }

    fun refreshNow() {
        scope.launch {
            try {
                if (TokenStore.current() == null) return@launch
                _unreadThreads.value = ChatRepository.threads().count { it.unread }
            } catch (_: Throwable) { /* keep last value */ }
        }
    }

    private suspend fun maybePostMessageNotification(ev: ChatEvent) {
        val msg = ev.message ?: return
        val me = TokenStore.current() ?: return
        // Don't ping for messages I sent myself.
        if (msg.senderId == me.userId) return
        // Don't ping if the user is already looking at this thread.
        if (ActiveThreadTracker.isActive(ev.threadId)) return

        val ctx = AlfcApplication.appContext

        // Android 13+: silently no-op if the user hasn't granted POST_NOTIFICATIONS.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val tapIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            ctx,
            ev.threadId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(ctx, AlfcApplication.CHAT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(msg.senderName)
            .setContentText(msg.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        // One notification per thread — newer messages replace older ones.
        NotificationManagerCompat.from(ctx).notify(ev.threadId.toInt(), notification)
    }
}
