package it.unipd.dei.esp2021.nottalk.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class PlayerService : Service()
{
    private var myPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var uri: Uri

    // Create an executor that executes tasks in a background thread.
    val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()

    val mainThreadExecutor : Executor
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mainExecutor
        } else {
            ContextCompat.getMainExecutor(this)
        }

    override fun onBind(intent: Intent): IBinder?
    {
        return null // Clients can not bind to this service
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int
    {
        backgroundExecutor.execute {
            val uriString: String? = intent.getStringExtra(URI_PATH)
            if (uriString != null) {
                Log.d("Player Service", uriString)
                uri = Uri.parse(uriString)
            } else {
                Log.d("Player Service", "nullUriPassed")
            }

            if (intent.getBooleanExtra(PLAY_START, false)) play()

            mainThreadExecutor.execute {
                val uriString: String? = intent.getStringExtra(URI_PATH)
                if (uriString != null) {
                    Log.d("Player Service2", uriString)
                    uri = Uri.parse(uriString)
                } else {
                    Log.d("Player Service2", "nullUriPassed")
                }

                if (intent.getBooleanExtra(PLAY_START, false)) play()
            }
        }
        return START_STICKY
    }

    override fun onCreate()
    {
        super.onCreate()
    }

    private fun play()
    {
        if (isPlaying) return
        isPlaying = true

        Log.d("PlayerServiceClass", "Playing")

        myPlayer = MediaPlayer.create(this, uri)
        Log.d("PlayerServiceClass", "Playing2")

        //myPlayer = MediaPlayer.create(this, R.raw.doowackadoo)
        myPlayer!!.isLooping = true
        // myPlayer holds the PARTIAL_WAKE_LOCK lock to ensure that the CPU continues running
        // during playback. myPlayer holds the lock while playing and releases it when paused
        // or stopped
        myPlayer!!.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        myPlayer!!.start()
        if (myPlayer!!.isPlaying)
            Log.d("PlayerService", "Is really Playing")
        // I used the not-null assertion operator (!!) instead of the elvis operator (?)
        // for the mutable property myPlayer so the app crashes if the MediaPlayer is not
        // available, hence the user realizes that something went wrong

        // Build a notification with basic info about the song
        val notificationBuilder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(applicationContext, "notTalk")
            else  // Deprecation warning left on purpose for educational reasons
                Notification.Builder(applicationContext)
        notificationBuilder.setContentTitle("NotTalkAudio")
        notificationBuilder.setContentText("NotTalkAudio")
        notificationBuilder.setSmallIcon(android.R.drawable.ic_media_play)
        val notification = notificationBuilder.build() // Requires API level 16
        // Runs this service in the foreground,
        // supplying the ongoing notification to be shown to the user
        val notificationID = 5786423 // An ID for this notification unique within the app
        startForeground(notificationID, notification)

    }

    private fun stop()
    {
        if (isPlaying) {
            isPlaying = false
            myPlayer?.release()
            myPlayer = null
            stopForeground(true)
        }
    }

    override fun onDestroy()
    {
        stop()
        super.onDestroy()
    }

    companion object
    {
        private const val CHANNEL_ID = "simplebgplayer"
        const val PLAY_START = "BGPlayStart"
        const val PLAY_STOP = "BGPlayStop"
        const val URI_PATH = "uriPath"
    }
}