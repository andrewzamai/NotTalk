package it.unipd.dei.esp2021.nottalk.remote

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.MediaSession2Service
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import it.unipd.dei.esp2021.nottalk.*
import it.unipd.dei.esp2021.nottalk.util.FileManager
import it.unipd.dei.esp2021.nottalk.util.AppNotificationManager
import it.unipd.dei.esp2021.nottalk.util.NotificationActionsReceiver
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class SyncService : Service() {
    // Create an executor that executes tasks in a background thread.
    private val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val pendingIntent: PendingIntent =
            Intent(applicationContext, ItemDetailHostActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(applicationContext, 0, notificationIntent, 0) }
        val deleteIntent: PendingIntent =
            Intent(applicationContext, NotificationActionsReceiver::class.java).let { notificationIntent ->
                //notificationIntent.putExtra("requestCode",ItemDetailHostActivity.SERVICE_STOP)
                notificationIntent.action = STOP_SERVICE
                PendingIntent.getBroadcast(applicationContext, ItemDetailHostActivity.SERVICE_STOP, notificationIntent, 0) }
        val notificationLayout = RemoteViews(packageName,R.layout.foreground_notification)
        notificationLayout.setOnClickPendingIntent(R.id.fgn_ButtonClose,deleteIntent)
        val notification: Notification = NotificationCompat.Builder(applicationContext, NotTalkApplication.FOREGROUND_CHANNEL)
            .setContentTitle("Service is running")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_nt_notification_logo)
            .setContentIntent(pendingIntent)
            .setTicker("NotTalk service is running")
            //.setColor(getColor(R.color.NT_purple2))
            //.setColorized(true)
            //.addAction(R.drawable.ic_close,"Close application", deleteIntent)
            //.addAction(NotificationCompat.Action.Builder(R.drawable.ic_close,"Close application", deleteIntent).build())
            //.setStyle(androidx.media.app.NotificationCompat.MediaStyle().MediaStyle().setShowActionsInCompactView(0))
            .setContent(notificationLayout)
            .build()
        startForeground(999999, notification)
        /*
        backgroundExecutor.execute {
            // Your code logic goes here.
            Log.d("SyncService", "1")
            // Update UI on the main thread
            mainThreadExecutor.execute {
                // You code logic goes here.
                Log.d("SyncService", "2")
            }
        }
        */
        backgroundExecutor.scheduleAtFixedRate({
            try {
                val sp1 = getSharedPreferences("notTalkPref", MODE_PRIVATE)
                val username = sp1.getString("thisUsername", "")
                val uuid = sp1.getString("uuid", "")

                val sa = ServerAdapter()
                val response = sa.checkMsg(username!!, uuid!!)
                if (response.first.isNotEmpty()) {
                    for (msg in response.first) {
                        if (msg.type == "file") {
                            val path = FileManager.saveFileToStorage(
                                applicationContext,
                                msg.text,
                                msg.fileName!!,
                                msg.mimeType!!
                            )
                            msg.text = path
                        }
                        msg.read = false
                    }
                    //val cd = ChatDatabase.getDatabase(applicationContext)
                    val cd = NotTalkRepository.get()
                    cd.insertMessages(response.first)
                    sa.deleteMsg(username!!, uuid!!, response.second)
                    Thread(Runnable {
                        for (msg in response.first) {
                            if (!cd.existsRelation(username, msg.fromUser)) {
                                cd.insertUser(msg.fromUser)
                                cd.createRelation(msg.fromUser)
                            }
                        }
                    }).start()

                    val nm = AppNotificationManager.get()
                    for (i in response.first) {
                        val senderMes = nm.getSenderMes()
                        if(nm.canBubble(i.fromUser)) {
                            if (!senderMes.contains(i.fromUser)) {
                                nm.showNotification(i, true, false)
                            } else {
                                nm.showNotification(i, true, true)
                            }
                            nm.addSenderMes(i.fromUser)
                        }else{
                            if (!senderMes.contains(i.fromUser)) {
                                nm.showNotification(i, false, false)
                            } else {
                                nm.showNotification(i, false, true)
                            }
                            nm.addSenderMes(i.fromUser)
                        }
                    }
                }
            }
            catch(ex:Exception){
                println(ex.message)
                ex.printStackTrace()
            }

        }, 0, DEFAULT_SYNC_INTERVAL, TimeUnit.SECONDS)

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        backgroundExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        const val DEFAULT_SYNC_INTERVAL = 5.toLong()

        const val STOP_SERVICE = "stopService"
    }

    val mainThreadExecutor : Executor
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mainExecutor
        } else {
            ContextCompat.getMainExecutor(this)
        }
}