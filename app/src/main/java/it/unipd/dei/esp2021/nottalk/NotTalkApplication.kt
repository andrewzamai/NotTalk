package it.unipd.dei.esp2021.nottalk

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

import it.unipd.dei.esp2021.nottalk.util.AppNotificationManager

class NotTalkApplication : Application() {

    // on application creation initializes (only one time) NotTalkRepository
    // then singleton pattern in NotTalkRepository will provide the instance when asked via get function
    override fun onCreate() {
        super.onCreate()

        // initializes a NotTalkRepository object
        NotTalkRepository.initialize(this)
        // initializes a AppNotificationManager object
        AppNotificationManager.initialize(applicationContext)

        // create a channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Messages Notification Channel"
            val importance = NotificationManager.IMPORTANCE_HIGH // must be high to have ChatBubbles
            val channel = NotificationChannel(MESSAGE_NOTIFICATION_CHANNEL, name, importance)
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // create a channel for audio/music messages
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Audio Notification Channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(AUDIO_NOTIFICATION_CHANNEL, name, importance)
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // create a channel for foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Foreground Notification Channel"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(FOREGROUND_CHANNEL, name, importance)
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

    }

    companion object {
        const val AUDIO_NOTIFICATION_CHANNEL = "audioNotTalk"
        const val FOREGROUND_CHANNEL = "foregroundNotTalk"
        const val MESSAGE_NOTIFICATION_CHANNEL = "notTalk"
    }

}