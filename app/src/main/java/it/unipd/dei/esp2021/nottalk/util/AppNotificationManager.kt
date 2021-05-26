package it.unipd.dei.esp2021.nottalk.util

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.view.textclassifier.ConversationActions
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

import androidx.core.graphics.drawable.toIcon
import androidx.core.net.toUri
import it.unipd.dei.esp2021.nottalk.*
import it.unipd.dei.esp2021.nottalk.database.User
import it.unipd.dei.esp2021.nottalk.database.UserDao
import java.lang.IllegalStateException

class AppNotificationManager(private val context: Context){

    companion object{

        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: AppNotificationManager? = null

        fun initialize(context: Context){
            if(INSTANCE==null) {
                INSTANCE = AppNotificationManager(context)
            }
        }
        fun get(): AppNotificationManager {
            return INSTANCE?: throw IllegalStateException("NotificationManager must be initialized!")
        }

        // constants
        private const val CHANNEL_NEW_MESSAGES = "new_messages"
        private const val REQUEST_CONTENT = 1
        private const val REQUEST_BUBBLE = 2
    }

    private val notificationManager: NotificationManager =
        context.getSystemService() ?: throw IllegalStateException()

    private val shortcutManager: ShortcutManager =
        context.getSystemService() ?: throw IllegalStateException()

    fun setUpNotificationChannel(){
        if(notificationManager.getNotificationChannel(CHANNEL_NEW_MESSAGES) == null){
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_NEW_MESSAGES,
                    context.getString(R.string.channel_new_messages),
                    NotificationManager.IMPORTANCE_HIGH //IMPORTANCE_HIGH is necessary for the chat Bubbles
                ).apply {
                    description = context.getString(R.string.channel_new_messages_description)
                }
            )
        }
    }

    @WorkerThread
    fun showNotification(chat: ChatViewModel, fromUser: Boolean, update: Boolean = false){

        val user = Person.Builder().setName(context.getString(R.string.sender_you)).build()

        val icon = NotTalkRepository.get().findIconByUsername(chat.getOtherUser()).toIcon()
        val person = Person.Builder().setName(chat.getOtherUser()).setIcon(icon).build()

        val contentUri = "https://nottalk.esp2021.dei.unipd.it/username/${chat.getOtherUser()}".toUri()

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_BUBBLE,
            Intent(context, BubbleActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setData(contentUri),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = Notification.Builder(context, CHANNEL_NEW_MESSAGES)
            .setBubbleMetadata(
                Notification.BubbleMetadata.Builder(pendingIntent, icon) //VUOLE API 30 - VUOLE ICONA DELLA PERSONA CHE SPEDISCE
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

            .setContentTitle(chat.getOtherUser())
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setLocusId(LocusId(chat.getOtherUser()))
            .addPerson(person)
            .setShowWhen(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    REQUEST_CONTENT,
                    Intent(context, ItemDetailHostActivity::class.java)
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
                                .setData(contentUri)
                                .putExtra("otherUser", chat.getOtherUser()),
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                    .addRemoteInput(
                        RemoteInput.Builder(ReplyReceiver.KEY_TEXT_REPLY)
                            .setLabel(context.getString(R.string.hint_input))
                            .build()
                    )
                    .setAllowGeneratedReplies(true)
                    .build()
            )

            //setStyle da implementare (facoltativo)
    }

    private fun dismissNotification(id: Long) {
        notificationManager.cancel(id.toInt())
    }

    fun canBubble(user: User): Boolean {
        val channel = notificationManager.getNotificationChannel(
            CHANNEL_NEW_MESSAGES,
            user.username
        )
        return notificationManager.areBubblesAllowed() || channel?.canBubble() == true
    }

    fun updateNotification(chat: ChatViewModel, chatId: Long, prepopulatedMsgs: Boolean) {
        if (!prepopulatedMsgs) {
            showNotification(chat, fromUser = false, update = true)
        } else {
            dismissNotification(chatId)
        }
    }


}