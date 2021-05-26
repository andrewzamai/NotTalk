package it.unipd.dei.esp2021.nottalk.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationActionsReceiver() : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("NotificationActionReceiver", "NotAction onReceive")
        val intent = Intent(context, PlayerService::class.java)
        intent.putExtra("MY_ACTION", "PAUSE")
        context!!.startService(intent)
    }
}