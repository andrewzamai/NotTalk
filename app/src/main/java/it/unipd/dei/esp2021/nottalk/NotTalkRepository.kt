package it.unipd.dei.esp2021.nottalk

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import it.unipd.dei.esp2021.nottalk.database.ChatDatabase
import it.unipd.dei.esp2021.nottalk.database.Message
import it.unipd.dei.esp2021.nottalk.database.User
import it.unipd.dei.esp2021.nottalk.remote.ServerAdapter
import java.io.InputStream
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Executors

// the name of the database file
private const val DATABASE_NAME = "chat-database"

/*
 * NotTalkRepository is designed to act as a Singleton Pattern.
 * When a ChatDabase instance is needed use the get function to retrieve a reference to it.
 * You don't have to care about it's initialization being it first initialized when app starts in NotTalkApplication, before any other class.
 *
 * NotTalkRepository also encapsulates the logic for accessing data from a single or a set of sources:
 * similarly to an Adapter pattern store a reference to an object and provides functions which in turn call the right function.
 * Sometimes this functions need no more code, other times need access to a separated thread to execute.
 */
class NotTalkRepository private constructor(context: Context){

    private val database: ChatDatabase = Room.databaseBuilder(
        context.applicationContext,
        ChatDatabase::class.java,
        DATABASE_NAME
    )   .addCallback(ChatDatabaseCallback())
        .build()

    private class ChatDatabaseCallback(): RoomDatabase.Callback(){
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Thread(Runnable{
                val db = get()
                db.insertUser(User("Gianni"))
                db.insertUser(User("admin"))
            }).start()
        }
    }


    private val context = context
    // reference to a serverAdapter instance, one single instance accessed from a NotTalkRepository object
    private val server: ServerAdapter = ServerAdapter()

    // reference to an UserDao instance
    private val userDao = database.userDao()
    // reference to a MessageDao instance
    private val messageDao = database.messageDao()

    // single thread executor to perform functions as insert in database which needs not to stop the main (UI) thread
    private val executor = Executors.newSingleThreadExecutor()



// UserDao adapter functions

    fun getAllUsers(): LiveData<List<User>> = userDao.all // liveData enables to notify an observer about changes in the list

    fun insertUser(user: User) {
        executor.execute {
            userDao.insert(user)
        }
    }

    fun checkUser(username: String): Boolean{
        return userDao.doesExist(username)

    }

    fun insertMessages(messages: List<Message>) {
        executor.execute {
            messageDao.insertAll(messages)
        }
    }

    fun insertMessage(message: Message) {
        executor.execute {
            messageDao.insert(message)
        }
    }



// MessageDao adapter functions
    fun getConvo(thisUser: String, otherUser: String): LiveData<List<Message>> = messageDao.findConvo(thisUser, otherUser)



// ServerAdapter functions

    // to send a text message
    fun sendTextMessage(thisUsername: String, uuid: String, text: String, otherUsername: String){
        Thread(Runnable {
            // creates a message object
            val msg = Message(
                otherUsername,
                thisUsername,
                Calendar.getInstance().timeInMillis,
                "text",
                text
            )
            // adds it to the local database
            insertMessage(msg)
            // tries to send it to the server
            val response = server.sendTextMsg(msg.fromUser, uuid, msg.toUser, msg.date, msg.text)
            if (response != "ok") {
                //Toast.makeText(context.applicationContext, response, Toast.LENGTH_SHORT).show() // creates toast displaying error
                    Log.d("Server error", uuid)
                Log.d("Server error", response)
                this.deleteMessage(msg) // deletes it from local database if couldn't send it
            }
        }).start()
    }

    fun sendFileMessage(context: Context, uri: Uri, username: String, uuid: String, toUser: String){
        Thread(Runnable {
            val contentResolver = context.contentResolver
            val mimetype = contentResolver.getType(uri)
            val filename = DocumentFile.fromSingleUri(context, uri)?.name
            val fileInStream: InputStream
            val date = Calendar.getInstance().timeInMillis

            contentResolver.run {
                fileInStream = openInputStream(uri) ?: throw Exception("Error file")
            }
            val content = Base64.encodeToString(fileInStream.readBytes(), Base64.DEFAULT)
            val result = server.sendFileMsg(
                username,
                uuid,
                toUser,
                date,
                content,
                mimetype!!,
                filename!!
            )
            if (result == "ok") {
                val msg = Message(username, toUser, date, "file", uri.toString())
                msg.fileName = filename!!
                msg.mimeType = mimetype!!
                messageDao.insert(msg)
            }
        }).start()
    }

    // to delete a message from the database
    fun deleteMessage(message: Message){
        messageDao.delete(message)
    }



// Singleton Design Pattern for this class
    companion object{
        private var INSTANCE: NotTalkRepository? = null

        fun initialize(context: Context){
            if(INSTANCE == null){
                INSTANCE = NotTalkRepository(context)
            }
        }

        fun get(): NotTalkRepository{
            return INSTANCE?:
            throw IllegalStateException("NotTalkRepository must be initialized!") //(initialized in NotTalkApplication at application start)
        }
    }


}