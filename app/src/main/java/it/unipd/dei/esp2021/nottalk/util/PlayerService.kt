package it.unipd.dei.esp2021.nottalk.util

import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getBroadcast
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.PlaybackState
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import it.unipd.dei.esp2021.nottalk.NotTalkApplication
import it.unipd.dei.esp2021.nottalk.NotTalkRepository
import it.unipd.dei.esp2021.nottalk.R
import it.unipd.dei.esp2021.nottalk.database.User

private const val PLAYER_TAG = "Player Service"

class PlayerService() : Service() {

    private var myPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var uri: Uri // Uri to the audio content to play
    private var username: String? = null // username of the audio media sender
    private lateinit var mediaSession: MediaSessionCompat // to let MediaStyle Notification be in media controls must have a valid MediaSessionCompat token

    override fun onBind(intent: Intent): IBinder? {
        return null // Clients can not bind to this service
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val uriString: String? = intent.getStringExtra(URI_PATH)
        if (uriString != null) {

            Log.d(PLAYER_TAG, uriString)
            uri = Uri.parse(uriString)
            username = intent.getStringExtra(USERNAME)

            if (intent.getBooleanExtra(PLAYER_START, false)) play()

        } else {
            Log.d(PLAYER_TAG, "nullUriPassed")
            when (intent.action) {

                PLAYER_PAUSE -> {
                    if (isPlaying) {
                        myPlayer!!.pause()
                        // set information about playbackstate
                        mediaSession.setPlaybackState(
                            PlaybackStateCompat.Builder()
                                .setState(
                                    PlaybackStateCompat.STATE_PAUSED,
                                    // Playback position
                                    myPlayer!!.currentPosition.toLong(),
                                    // Playback speed
                                    myPlayer!!.playbackParams.speed
                                )
                                // isSeekable
                                //.setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                                .build()
                        )
                    }
                }
                PLAYER_RESTART -> {
                    myPlayer!!.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    myPlayer!!.start()
                    mediaSession.setPlaybackState(
                        PlaybackStateCompat.Builder()
                            .setState(
                                PlaybackStateCompat.STATE_PLAYING,
                                // Playback position
                                myPlayer!!.currentPosition.toLong(),
                                // Playback speed
                                myPlayer!!.playbackParams.speed
                            )
                            // isSeekable
                            //.setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                            .build()
                    )

                }
                PLAYER_START -> {
                    myPlayer!!.start()
                    mediaSession.setPlaybackState(
                        PlaybackStateCompat.Builder()
                            .setState(
                                PlaybackStateCompat.STATE_PLAYING,
                                // Playback position
                                myPlayer!!.currentPosition.toLong(),
                                // Playback speed
                                myPlayer!!.playbackParams.speed
                            )
                            // isSeekable
                            //.setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                            .build()
                    )
                }
            }
        }

        return START_STICKY
    }


    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, PLAYER_TAG)
    }

    private fun play() {

        Log.d(PLAYER_TAG, "onPlay called")

        if (isPlaying)
            return
        isPlaying = true

        myPlayer = MediaPlayer.create(this, uri) // creates and ALSO prepares media player with passed URI
        Log.d(PLAYER_TAG, "MediaPlayer created and prepared")

        // myPlayer holds the PARTIAL_WAKE_LOCK lock to ensure that the CPU continues running during playback.
        // myPlayer holds the lock while playing and releases it when paused or stopped
        myPlayer!!.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

        myPlayer!!.start()
        if (myPlayer!!.isPlaying)
            Log.d(PLAYER_TAG, "Is really playing")

        val otherUsername = username
        if (otherUsername != null) {
            Log.d(PLAYER_TAG, otherUsername)
        }
        // retrieves User from its username (usernames are unique)
        val user: User? = username?.let { NotTalkRepository.get().findByUsername(it) }
        Log.d(PLAYER_TAG, username.toString())
        // retrieves its profile Bitmap picture to set it as notification media picture
        val bArray = user?.picture
        val bitmap = if (bArray != null) {
            BitmapFactory.decodeByteArray(bArray, 0, bArray.size)
        } else {
            getDrawable(R.drawable.ic_avatar)?.toBitmap()
        }

        // set information about sender, profile picture, audio duration
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                // Title
                .putString(MediaMetadata.METADATA_KEY_TITLE, getString(R.string.PlayerTitle))
                // Artist
                .putString(MediaMetadata.METADATA_KEY_ARTIST, getString(R.string.PlayerArtist) + " " + username)
                // Profile Pic
                .putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                // Duration
                .putLong(MediaMetadata.METADATA_KEY_DURATION, myPlayer!!.duration.toLong())
            .build()
        )

        // set information about playbackstate
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    // Playback position
                    myPlayer!!.currentPosition.toLong(),
                    // Playback speed
                    myPlayer!!.playbackParams.speed
                )
                // isSeekable
                //.setActions(PlaybackStateCompat.ACTION_SEEK_TO)
            .build()
        )

        mediaSession.isActive = true


        // Build a notification
        val notification = NotificationCompat
            .Builder(this, NotTalkApplication.AUDIO_NOTIFICATION_CHANNEL)
            // Show controls on lock screen even when user hides sensitive content
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            // Add media control buttons that invoke intents
            .addAction(android.R.drawable.ic_menu_revert, "Restart", getBroadcast(this, 0, Intent(this, NotificationActionsReceiver::class.java).setAction(PLAYER_RESTART), 0)) // rePlay from beginning
            .addAction(android.R.drawable.ic_media_play, "Play", getBroadcast(this, 0, Intent(this, NotificationActionsReceiver::class.java).setAction(PLAYER_START), 0))
            .addAction(android.R.drawable.ic_media_pause, "Pause", getBroadcast(this, 0, Intent(this, NotificationActionsReceiver::class.java).setAction(PLAYER_PAUSE), 0))
            // Apply the media style template
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSession.sessionToken))
            .setColor(resources.getColor(R.color.purple_500, resources.newTheme()))
            .build()
        startForeground(username.hashCode(), notification) // startForeground MUST notify the user about starting playing

    }

    private fun stop() {
        if (isPlaying) {
            isPlaying = false
            myPlayer?.release()
            myPlayer = null
            stopForeground(true)
            mediaSession.isActive = false
        }
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    companion object {
        const val PLAYER_START = "it.dei.esp2021.nottalk.util.PLAYER_START"
        const val PLAYER_STOP = "it.dei.esp2021.nottalk.util.PLAYER_STOP"
        const val PLAYER_RESTART = "it.dei.esp2021.nottalk.util.PLAYER_RESTART"
        const val PLAYER_PAUSE = "it.dei.esp2021.nottalk.util.PLAYER_PAUSE"
        const val URI_PATH = "uriPath"
        const val USERNAME = "username"
    }

}