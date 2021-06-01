package it.unipd.dei.esp2021.nottalk.util

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toIcon
import androidx.core.net.toUri
import it.unipd.dei.esp2021.nottalk.*
import it.unipd.dei.esp2021.nottalk.database.Message
import it.unipd.dei.esp2021.nottalk.database.User
import java.lang.IllegalStateException

class AppNotificationManager(private val context: Context){

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: AppNotificationManager? = null

        private const val CHANNEL_NEW_MESSAGES = "notTalk"

        private const val REQUEST_CONTENT = 1
        private const val REQUEST_BUBBLE = 2

        private val senderMes = mutableListOf<String>()

        fun initialize(context: Context){
            if(INSTANCE ==null) {
                INSTANCE = AppNotificationManager(context)
            }
        }

        fun get(): AppNotificationManager {
            return INSTANCE ?: throw IllegalStateException("NotificationManager must be initialized!")
        }

    }

    private val notificationManager: NotificationManager =
        context.getSystemService() ?: throw IllegalStateException()

    fun getSenderMes(): List<String>{
        return senderMes
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun showNotification(pendingMessages: Message, fromUser: Boolean, update: Boolean = false){

        senderMes.add(pendingMessages.fromUser)

        val contentUri = "https://nottalk.esp2021.dei.unipd.it/username/${pendingMessages.fromUser}".toUri() // uri con username primo messaggio

        val user = Person.Builder().setName(context.getString(R.string.sender_you)).build()

        val icon = NotTalkRepository.get().findIconByUsername(pendingMessages.fromUser).toIcon()
        val person = Person.Builder().setName(pendingMessages.fromUser).setIcon(icon).build()


        val chatId = NotTalkRepository.get().getByUsers(pendingMessages.toUser, pendingMessages.fromUser).id

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CONTENT,
            Intent(context, BubbleActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra("chatId",chatId)
                .putExtra("otherUser", pendingMessages.fromUser),
            PendingIntent.FLAG_UPDATE_CURRENT
        )


        val notification = Notification
            .Builder(context, CHANNEL_NEW_MESSAGES)
            .setBubbleMetadata(
                Notification.BubbleMetadata.Builder(pendingIntent, icon)
                    .setDesiredHeight(context.resources.getDimensionPixelSize(R.dimen.bubble_height))
                    .apply{
                        if(fromUser){
                            setAutoExpandBubble(true)
                        }
                        if(fromUser || update){
                            setSuppressNotification(true)
                        }
                    }
                    .build()
            )

            .setContentTitle(pendingMessages.fromUser)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setShortcutId(pendingMessages.fromUser)
            .setLocusId(LocusId(pendingMessages.fromUser))
            .addPerson(person)
            .setShowWhen(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    REQUEST_CONTENT,
                    Intent(context, ItemDetailHostActivity::class.java)
                        //.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(contentUri),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            //This action permits to reply
            .addAction(
                Notification.Action
                    .Builder(
                        Icon.createWithResource(context, android.R.drawable.ic_menu_send),
                        context.getString(R.string.label_reply),
                        PendingIntent.getBroadcast(
                            context,
                            REQUEST_CONTENT,
                            Intent(context, ReplyReceiver::class.java)
                                .putExtra("chatId",chatId)
                                .putExtra("otherUser", pendingMessages.fromUser),
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                    .addRemoteInput(
                        RemoteInput
                            .Builder(ReplyReceiver.KEY_TEXT_REPLY)
                            .setLabel(context.getString(R.string.hint_input))
                            .build()
                    )
                    .setAllowGeneratedReplies(true)
                    .build()
            )

            .setStyle(
                Notification.MessagingStyle(user)
                    .apply {
                        val m = Notification.MessagingStyle.Message(
                            pendingMessages.text,
                            pendingMessages.date,
                            person
                        ).apply {
                            if (pendingMessages.type == "file") {
                                setData(pendingMessages.mimeType, Uri.parse(pendingMessages.text))
                            }
                        }

                            if (update) {
                                addHistoricMessage(m)
                            } else {
                                addMessage(m)

                        }

                    }
                    .setGroupConversation(false)
            )
            .setWhen(pendingMessages.date)
        notificationManager!!.notify(chatId!!, notification.build())

    }

    fun canBubble(user: String): Boolean {
        val channel = notificationManager.getNotificationChannel(
            CHANNEL_NEW_MESSAGES,
            user
        )
        return notificationManager.areBubblesAllowed() || channel?.canBubble() == true
    }

    fun updateNotification(chatId: Int) {
        senderMes.remove(NotTalkRepository.get().getById(chatId).otherUser)
        dismissNotification(chatId)
    }

    private fun dismissNotification(id: Int) {
        notificationManager!!.cancel(id)
    }


}