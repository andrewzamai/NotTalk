package it.unipd.dei.esp2021.nottalk.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationActionsReceiver() : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("NotificationActionReceiver", "onReceive")

        val serviceIntent = Intent(context, PlayerService::class.java)

        when (intent?.action) {
            PlayerService.PLAYER_PAUSE -> serviceIntent.action = PlayerService.PLAYER_PAUSE
            PlayerService.PLAYER_START -> serviceIntent.action = PlayerService.PLAYER_START
            PlayerService.PLAYER_RESTART -> serviceIntent.action = PlayerService.PLAYER_RESTART
        }
        context?.startService(serviceIntent)
    }

}