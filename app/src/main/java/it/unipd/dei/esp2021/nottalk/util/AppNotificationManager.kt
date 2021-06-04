package it.unipd.dei.esp2021.nottalk.util

import android.annotation.SuppressLint
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.LocusIdCompat
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toIcon
import androidx.core.net.toUri
import it.unipd.dei.esp2021.nottalk.*
import it.unipd.dei.esp2021.nottalk.database.Message
import it.unipd.dei.esp2021.nottalk.database.User
import it.unipd.dei.esp2021.nottalk.database.UserRelationDao
import java.lang.IllegalStateException
import java.util.Objects.compare
import java.util.Objects.equals

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

    private val shortcutManager: ShortcutManager =
        context.getSystemService() ?: throw IllegalStateException()

    fun getSenderMes(): List<String>{
        return senderMes
    }

    fun addSenderMes(sender: String) {
        senderMes.add(sender)
    }


    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    fun updateShortcuts(pendingMessages: Message){
        //val chatId = NotTalkRepository.get().getByUsers(pendingMessages.toUser, pendingMessages.fromUser)?.id

        val contentUri = "https://nottalk.esp2021.dei.unipd.it/username/${pendingMessages.fromUser}".toUri()

        ShortcutManagerCompat.addDynamicShortcuts(
            context, mutableListOf(
                ShortcutInfoCompat.Builder(context, NotTalkRepository.get().findByUsername(pendingMessages.fromUser).username)
                    .setLongLived(true)
                    .setLocusId(LocusIdCompat(NotTalkRepository.get().findByUsername(pendingMessages.fromUser).username))
                    .setShortLabel(pendingMessages.fromUser)
                    .setPerson(androidx.core.app.Person.fromAndroidPerson(Person
                        .Builder()
                        .setName(pendingMessages.fromUser)
                        .setIcon(NotTalkRepository.get().findIconByUsername(pendingMessages.fromUser).toIcon())
                        .build()))
                    .setIntent( Intent(context, ItemDetailHostActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(contentUri) )
                    .setIcon(IconCompat.createWithBitmap((NotTalkRepository.get().findIconByUsername(pendingMessages.fromUser))))
                    .setCategories(setOf("com.example.android.bubbles.category.TEXT_SHARE_TARGET"))
                    .build()
            )
        )


        /*
        var shortcuts = NotTalkRepository.get().getAllOtherUserByUsers(pendingMessages.toUser).map {user ->
            val icon = Icon.createWithAdaptiveBitmap(
                context.resources.assets.open(NotTalkRepository.get().findIconByUsername(pendingMessages.fromUser).toIcon().toString()).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            )
            ShortcutInfo
                .Builder(
                    context,
                    NotTalkRepository.get().findByUsername(pendingMessages.fromUser).username
                )
                .setLocusId(
                    LocusId(
                        NotTalkRepository.get().findByUsername(pendingMessages.fromUser).username
                    )
                )
                .setActivity(ComponentName(context, ItemDetailHostActivity::class.java))
                .setLongLived(true)
                .setIntent(
                    Intent(context, ItemDetailHostActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(contentUri)
                )
                .setPerson(Person
                    .Builder()
                    .setName(pendingMessages.fromUser)
                    .setIcon(icon)
                    .build())
                .setShortLabel(pendingMessages.fromUser)
                .setLongLabel(pendingMessages.text)
                .setIcon(icon)
                .setCategories(setOf("com.example.android.bubbles.category.TEXT_SHARE_TARGET"))
                .build()
        }
        if (NotTalkRepository.get().findByUsername(pendingMessages.fromUser) != null) {
            shortcuts = shortcuts.sortedByDescending { it.id == pendingMessages.fromUser }
        }
        // Truncate the list if we can't show all of our contacts.
        val maxCount = shortcutManager.maxShortcutCountPerActivity
        if (shortcuts.size > maxCount) {
            shortcuts = shortcuts.take(maxCount)
        }
        shortcutManager.addDynamicShortcuts(shortcuts)

         */

    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun showNotification(pendingMessages: Message, fromUser: Boolean, update: Boolean = false){

        senderMes.add(pendingMessages.fromUser)

        updateShortcuts(pendingMessages)

        val contentUri = "https://nottalk.esp2021.dei.unipd.it/username/${pendingMessages.fromUser}".toUri() // uri con username primo messaggio

        val user = Person.Builder().setName(context.getString(R.string.sender_you)).build()

        val icon = NotTalkRepository.get().findIconByUsername(pendingMessages.fromUser).toIcon()
        val person = Person
            .Builder()
            .setName(pendingMessages.fromUser)
            .setImportant(true)
            .setIcon(icon)
            .build()


        val chatId = NotTalkRepository.get().getByUsers(pendingMessages.toUser, pendingMessages.fromUser)?.id

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CONTENT,
            Intent(context, BubbleActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra("thisUser",pendingMessages.toUser)
                .putExtra("otherUser", pendingMessages.fromUser),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val target = Intent(context, BubbleActivity::class.java)
        val bubbleIntent = PendingIntent.getActivity(context, 0, target, 0)

        val bubbleData = Notification
            .BubbleMetadata.Builder(pendingIntent, icon)
            .setDesiredHeight(context.resources.getDimensionPixelSize(R.dimen.bubble_height))
            .setIntent(bubbleIntent)
            .setAutoExpandBubble(true)
            .setSuppressNotification(true)
            .build()


        val notification = Notification
            .Builder(context, CHANNEL_NEW_MESSAGES)
            .setBubbleMetadata(bubbleData)
            .setContentTitle(pendingMessages.fromUser)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setShortcutId(NotTalkRepository.get().findByUsername(pendingMessages.fromUser).username)
            //.setLocusId(LocusId(NotTalkRepository.get().findByUsername(pendingMessages.fromUser).username))
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
                        )
                            .apply {
                                if (pendingMessages.type == "file") {
                                    setData(
                                        pendingMessages.mimeType,
                                        Uri.parse(pendingMessages.text)
                                    )
                                }
                            }

                        val messagesList = NotTalkRepository.get().getConvoNotLiveData(pendingMessages.fromUser, pendingMessages.toUser)
                        val lastReadMessage = messagesList?.first() { equals(it.read, true)}
                        val lastReadMessageIndex = messagesList.indexOf(lastReadMessage)
                        Log.d("AppNotificationManager", messagesList.toString())
                        Log.d("AppNotificationManager", lastReadMessage.toString())
                        Log.d("AppNotificationManager", lastReadMessageIndex.toString())

                        var i = lastReadMessageIndex-1
                        while (i>0) {
                            val senderPerson = if (messagesList[i].fromUser == pendingMessages.fromUser) {
                                person
                            } else {
                                user
                            }
                            addHistoricMessage(Notification.MessagingStyle.Message(
                                messagesList[i].text,
                                messagesList[i].date,
                                senderPerson)
                            )
                            i--
                        }

                        addMessage(m)

                    }



                        /*
                        if (!update) {
                            Log.d("AppNotificationManager", "Update=false called")
                            //addHistoricMessage(m)
                            addMessage(m)
                        } else {
                            Log.d("AppNotificationManager", "Update=true called")
                            Log.d("AppNotificationManager", "historicMessagesSize ${historicMessages.size}" )
                            for (i in historicMessages) {
                                addMessage(i)
                            }
                            addMessage(m)
                            addHistoricMessage(m)
                        }

                         */


                        /*
                        if (!update) {
                            Log.d("AppNotificationManager", "Update=false called")
                            addMessage(m)
                        } else {
                            Log.d("AppNotificationManager", "Update=true called")

                            val messagesList = NotTalkRepository.get().getConvoNotLiveData(pendingMessages.fromUser, pendingMessages.toUser)
                            val lastReadMessage = messagesList?.first() { equals(it.read, true)}
                            val lastReadMessageIndex = messagesList.indexOf(lastReadMessage)
                            Log.d("AppNotificationManager", messagesList.toString())
                            Log.d("AppNotificationManager", lastReadMessage.toString())
                            Log.d("AppNotificationManager", lastReadMessageIndex.toString())

                            for (i in 0..lastReadMessageIndex) {
                                val senderPerson = if (messagesList[i].fromUser == pendingMessages.fromUser) {
                                    person
                                } else {
                                    user
                                }
                                addHistoricMessage(Notification.MessagingStyle.Message(
                                    messagesList[i].text,
                                    messagesList[i].date,
                                    senderPerson))
                            }

                            /*
                            for (i in 0..5) {
                                val senderPerson = if (messagesList[i].fromUser == pendingMessages.fromUser) {
                                    person
                                } else {
                                    user
                                }
                                addHistoricMessage(Notification.MessagingStyle.Message(
                                    messagesList[i].text,
                                    messagesList[i].date,
                                    senderPerson))
                            }
                            */


                            addMessage(m)

                            */




                    .setGroupConversation(false)
            )
            .setWhen(pendingMessages.date)
        //if (update) {
          //  builder.setOnlyAlertOnce(true)
        //}
        notificationManager.notify(chatId!!, notification.build())

    }

    fun canBubble(user: String): Boolean {
        val channel = notificationManager.getNotificationChannel(
            CHANNEL_NEW_MESSAGES,
            user
        )
        return notificationManager.areBubblesAllowed() || channel?.canBubble() == true
    }

    fun updateNotification(chatId: Int) {
        senderMes.remove(NotTalkRepository.get().getById(chatId)?.otherUser)
        dismissNotification(chatId)
    }

    private fun dismissNotification(id: Int) {
        notificationManager!!.cancel(id)
    }


}