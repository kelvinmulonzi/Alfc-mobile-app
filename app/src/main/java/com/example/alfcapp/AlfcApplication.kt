package com.example.alfcapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

class AlfcApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        createChatNotificationChannel()
    }

    private fun createChatNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            CHAT_CHANNEL_ID,
            "Chat messages",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Plays a sound when a new chat message arrives."
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), attrs)
            enableVibration(true)
        }
        mgr.createNotificationChannel(channel)
    }

    companion object {
        const val CHAT_CHANNEL_ID = "chat_messages"

        lateinit var appContext: Context
            private set
    }
}
