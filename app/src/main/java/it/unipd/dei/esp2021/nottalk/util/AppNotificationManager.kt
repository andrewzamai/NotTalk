package it.unipd.dei.esp2021.nottalk.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import it.unipd.dei.esp2021.nottalk.ItemDetailHostActivity
import it.unipd.dei.esp2021.nottalk.NotTalkRepository
import it.unipd.dei.esp2021.nottalk.database.Message
import java.lang.IllegalStateException

class AppNotificationManager(context: Context){
    private val pendingMessages = mutableListOf<Message>()
    private val context = context

    fun clear(){
        pendingMessages.clear()
    }

    fun append(list: List<Message>){
        pendingMessages.addAll(list)
    }

    fun sendNotification(){
        val notification = NotificationCompat
            .Builder(context, "notTalk")
            .setTicker("Nuovi messaggi")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Nuovi messaggi")
            .setContentText("Hai ${pendingMessages.size} nuovi messaggi.")
            .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, ItemDetailHostActivity::class.java),0))
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(0, notification)
    }

    companion object{
        private var INSTANCE: AppNotificationManager? = null

        fun initialize(context: Context){
            if(INSTANCE==null) {
                INSTANCE = AppNotificationManager(context)
            }
        }
        fun get(): AppNotificationManager {
            return INSTANCE?: throw IllegalStateException("NotificationManager must be initialized!")
        }
    }
}