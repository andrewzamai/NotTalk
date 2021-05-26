package it.unipd.dei.esp2021.nottalk

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class ReplyReceiver : BroadcastReceiver(){
    companion object{
        const val KEY_TEXT_REPLY = "reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val repository: NotTalkRepository = NotTalkRepository.get()

        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        // The message typed in the notification reply.
        val input = results.getCharSequence(KEY_TEXT_REPLY)?.toString()
        val uri = intent.data ?: return
        val chatId = uri.lastPathSegment?.toLong() ?: return


        if (chatId > 0 && !input.isNullOrBlank()) {
            val sharedPref = context.getSharedPreferences("notTalkPref", AppCompatActivity.MODE_PRIVATE)
            val thisUser = sharedPref.getString("thisUsername", "")
            val uuid = sharedPref.getString("uuid", "")
            val otherUser = intent.getStringExtra("otherUser")
            repository.sendTextMessage(thisUser!!, uuid!!, input.toString(), otherUser!!)
            // We should update the notification so that the user can see that the reply has been
            // sent.
            repository.updateNotification(chatId)
        }
    }
}