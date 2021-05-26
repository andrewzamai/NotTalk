package it.unipd.dei.esp2021.nottalk.util

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.PlaybackState.ACTION_STOP
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.unipd.dei.esp2021.nottalk.NotTalkRepository
import it.unipd.dei.esp2021.nottalk.R
import it.unipd.dei.esp2021.nottalk.database.User
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val PLAYER_TAG = "Player Service"

//TODO: this service works in mainthread
class PlayerService() : Service()
{
    private var myPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var uri: Uri // Uri to the audio content to play
    private var username: String? = null // username of the audio media sender

    private lateinit var mediaSession: MediaSessionCompat


    //TODO: da eliminare
    private class MySessionCallback() : MediaSessionCompat.Callback() {
        override fun onPause() {
            super.onPause()
            Log.d("MySessionCallBack", "OnPause called")
        }

        override fun onStop() {
            super.onStop()
            Log.d("MySessionCallBack", "OnStop called")
        }
    }


    override fun onBind(intent: Intent): IBinder?
    {
        return null // Clients can not bind to this service
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int
    {
        val uriString: String? = intent.getStringExtra(URI_PATH)
        if (uriString != null) {
            Log.d(PLAYER_TAG, uriString)
            uri = Uri.parse(uriString)

        } else {
            Log.d(PLAYER_TAG, "nullUriPassed")
        }

        username = intent.getStringExtra(USERNAME)

        if (intent.getStringExtra("MY_ACTION") == "PAUSE") {
            if (myPlayer!!.isPlaying)
                myPlayer!!.pause()
            else
                myPlayer!!.start()
        }


        if (intent.getBooleanExtra(PLAY_START, false)) play()

        return START_STICKY
    }

    override fun onCreate()
    {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, PLAYER_TAG)
        //mediaSession.setMediaButtonReceiver(PendingIntent.getActivity(this, 0, Intent(applicationContext, ItemDetailHostActivity::class.java), 0))
        //mediaSession.setCallback(MySessionCallback())

    }

    private fun play()
    {
        if (isPlaying) return
        isPlaying = true

        Log.d(PLAYER_TAG, "onPlay called")

        myPlayer = MediaPlayer.create(this, uri) // creates and ALSO prepares media player with passed URI
        Log.d(PLAYER_TAG, "MediaPlayer created and prepared")

        //myPlayer!!.isLooping = true

        // myPlayer holds the PARTIAL_WAKE_LOCK lock to ensure that the CPU continues running
        // during playback. myPlayer holds the lock while playing and releases it when paused
        // or stopped
        myPlayer!!.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

        myPlayer!!.start()
        if (myPlayer!!.isPlaying)
            Log.d(PLAYER_TAG, "Is really Playing")

        val otherUsername = username
        if (otherUsername != null) {
            Log.d(PLAYER_TAG, otherUsername)
        }


        val user: User? = username?.let { NotTalkRepository.get().findByUsername(it) }


        username?.let { NotTalkRepository.get().findByUsername(it).toString() }?.let {
            Log.d(PLAYER_TAG,
                it
            )
        }

        val bArray = user?.picture
        var bitmap: Bitmap? = null
        if(bArray != null) {
            bitmap = BitmapFactory.decodeByteArray(bArray, 0, bArray.size)
        }


        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                // Title.
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Audio")
                // Artist.
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "from $username")
                // Album art
                .putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                //.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
                //.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, ) // TODO: get image of User
                // Duration.
                .putLong(MediaMetadata.METADATA_KEY_DURATION, myPlayer!!.duration.toLong())
                .build()
        )

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    // Playback position.
                    // Used to update the elapsed time and the progress bar.
                    myPlayer!!.currentPosition.toLong(),

                    // Playback speed.
                    // Determines the rate at which the elapsed time changes.
                    myPlayer!!.playbackParams.speed
                )

                // isSeekable.
                // Adding the SEEK_TO action indicates that seeking is supported
                // and makes the seekbar position marker draggable. If this is not
                // supplied seek will be disabled but progress will still be shown.
                //.setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                .build()
        )

        mediaSession.isActive = true


        // Build a notification

        val pauseIntent = Intent(applicationContext, PlayerService::class.java)
        //pauseIntent.putExtra("MY_ACTION", "PAUSE")
        pauseIntent.setAction(ACTION_STOP.toString())

        val notification = NotificationCompat
            .Builder(this, "audioNotTalk")
            // Show controls on lock screen even when user hides sensitive content.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            // Add media control buttons that invoke intents in your media service
            .addAction(android.R.drawable.ic_media_previous, "Previous", null) // #0
            .addAction(android.R.drawable.ic_media_pause, "Pause", PendingIntent.getBroadcast(this, 0, Intent(this, NotificationActionsReceiver::class.java), 0)) // TODO: FLAG CURRENT
            .addAction(android.R.drawable.ic_media_next, "Next", null) // #2
            // Apply the media style template
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1 /* #1: pause button \*/)
                .setMediaSession(mediaSession.sessionToken))
            .setColor(2)
            .build()

        startForeground(23, notification)
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(23, notification)
        //startForeground(23, notification)
        /*
        val notification = NotificationCompat.Builder(applicationContext, "notTalk")
            // Show controls on lock screen even when user hides sensitive content.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            // Add media control buttons that invoke intents in your media service
            .addAction(android.R.drawable.ic_media_previous, "Previous", null) // #0
            .addAction(android.R.drawable.ic_media_pause, "Pause", null) // #1
            .addAction(android.R.drawable.ic_media_next, "Next", null) // #2
            // Apply the media style template
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1 /* #1: pause button \*/)
                .setMediaSession(mediaSession!!.sessionToken))
            .setContentTitle("Wonderful music")
            .setContentText("My Awesome Band")
            //.setAutoCancel(true)
            //.setLargeIcon(android.R.drawable.ic_btn_speak_now)
            .build()
         */
        //val notificationManager = NotificationManagerCompat.from(applicationContext)
        //notificationManager.notify(23, notification)


        /*
        val notification = Notification.Builder(applicationContext, "notTalk")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Track title")
            .setContentText("Artist - Album")
            //.setLargeIcon(android.R.drawable.btn_star)
        .setStyle(Notification.MediaStyle().setMediaSession(null))
        .build()

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(23, notification)
        */

        /*
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
        */
    }

    private fun stop()
    {
        if (isPlaying) {
            isPlaying = false
            myPlayer?.release()
            myPlayer = null
            stopForeground(true)
            mediaSession.isActive = false
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
        const val USERNAME = "username"
    }


}