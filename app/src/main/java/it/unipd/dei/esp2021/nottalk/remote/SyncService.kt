package it.unipd.dei.esp2021.nottalk.remote

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.unipd.dei.esp2021.nottalk.ItemDetailFragment
import it.unipd.dei.esp2021.nottalk.ItemDetailHostActivity
import it.unipd.dei.esp2021.nottalk.NotTalkRepository
import it.unipd.dei.esp2021.nottalk.database.ChatDatabase
import it.unipd.dei.esp2021.nottalk.database.FileManager
import it.unipd.dei.esp2021.nottalk.util.AppNotificationManager
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class SyncService : Service() {
    // Create an executor that executes tasks in a background thread.
    val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        backgroundExecutor.execute {
            // Your code logic goes here.
            Log.d("SyncService", "1")
            // Update UI on the main thread
            mainThreadExecutor.execute {
                // You code logic goes here.
                Log.d("SyncService", "2")
            }
        }

        backgroundExecutor.scheduleAtFixedRate({
            try {
                val sp1 = getSharedPreferences("notTalkPref", MODE_PRIVATE)
                val username = sp1.getString("thisUsername", "")
                val uuid = sp1.getString("uuid", "")

                val sa = ServerAdapter()
                val response = sa.checkMsg(username!!,uuid!!)
                if(response.first.isNotEmpty()) {
                    for(msg in response.first){
                        if(msg.type=="file"){
                            val path = FileManager.saveFileToStorage(
                                applicationContext,
                                msg.text,
                                msg.fileName!!,
                                msg.mimeType!!)
                            msg.text=path
                        }
                    }
                    //val cd = ChatDatabase.getDatabase(applicationContext)
                    val cd = NotTalkRepository.get()
                    cd.insertMessages(response.first)
                    sa.deleteMsg(username!!, uuid!!, response.second)



                    val nm = AppNotificationManager.get()
                    nm.append(response.first)
                    nm.sendNotification()
                }
            }
            catch(ex: Exception){
                print(ex.message)
                ex.printStackTrace()
            }
            finally {
                //STUB
            }
            // Your code logic goes here
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
    }

    val mainThreadExecutor : Executor
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mainExecutor
        } else {
            ContextCompat.getMainExecutor(this)
        }
}