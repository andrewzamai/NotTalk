package it.unipd.dei.esp2021.nottalk.util

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import it.unipd.dei.esp2021.nottalk.ItemDetailHostActivity
import it.unipd.dei.esp2021.nottalk.remote.SyncService
import kotlin.system.exitProcess

class NotificationActionsReceiver() : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("NotificationActionReceiver", "onReceive")

        val serviceIntent = Intent(context, PlayerService::class.java)

        when (intent?.action) {
            PlayerService.PLAYER_PAUSE -> serviceIntent.action = PlayerService.PLAYER_PAUSE
            PlayerService.PLAYER_START -> serviceIntent.action = PlayerService.PLAYER_START
            PlayerService.PLAYER_RESTART -> serviceIntent.action = PlayerService.PLAYER_RESTART
            SyncService.STOP_SERVICE -> {
                context?.startActivity(Intent(context,ItemDetailHostActivity::class.java).let{
                    it.action = SyncService.STOP_SERVICE
                    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return
            }
        }
        context?.startService(serviceIntent)
    }

}