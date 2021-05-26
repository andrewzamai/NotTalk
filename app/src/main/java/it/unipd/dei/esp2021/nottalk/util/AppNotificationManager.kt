package it.unipd.dei.esp2021.nottalk.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import it.unipd.dei.esp2021.nottalk.ItemDetailHostActivity
import it.unipd.dei.esp2021.nottalk.NotTalkRepository
import it.unipd.dei.esp2021.nottalk.database.Message
import java.lang.IllegalStateException

class AppNotificationManager(private val context: Context){

    private val pendingMessages = mutableListOf<Message>()

    fun clear(){
        pendingMessages.clear()
    }

    fun append(list: List<Message>){
        pendingMessages.addAll(list)
    }


    fun setUpNotificationChannel(){

    }

    // A notification for all messages: DA ELIMINARE
    fun sendNotification(){

        val contentUri = "https://nottalk.esp2021.dei.unipd.it/username/${pendingMessages[0].fromUser}".toUri() // uri con username primo messaggio

        val notification = NotificationCompat
            .Builder(context,"notTalk")
            .setTicker("Nuovi messaggi")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Nuovi messaggi")
            .setContentText("Hai ${pendingMessages.size} nuovi messaggi.")
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    REQUEST_CONTENT,
                    Intent(context, ItemDetailHostActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(contentUri),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            //.setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, ItemDetailHostActivity::class.java),0))
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(0, notification)
    }



    companion object{

        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: AppNotificationManager? = null

        fun initialize(context: Context){
            if(INSTANCE==null) {
                INSTANCE = AppNotificationManager(context)
            }
        }
        fun get(): AppNotificationManager {
            return INSTANCE?: throw IllegalStateException("NotificationManager must be initialized!")
        }

        // constants
        private const val CHANNEL_NEW_MESSAGES = "new_messages"
        private const val REQUEST_CONTENT = 1
        private const val REQUEST_BUBBLE = 2
    }
}