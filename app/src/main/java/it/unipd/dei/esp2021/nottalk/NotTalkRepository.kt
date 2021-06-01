package it.unipd.dei.esp2021.nottalk

import android.R.attr.*
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import it.unipd.dei.esp2021.nottalk.database.*
import it.unipd.dei.esp2021.nottalk.remote.ServerAdapter
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.InputStream
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
class NotTalkRepository private constructor(private val context: Context){

    private val database: ChatDatabase = Room.databaseBuilder(
        context.applicationContext,
        ChatDatabase::class.java,
        DATABASE_NAME
    )   .addCallback(ChatDatabaseCallback())
        .fallbackToDestructiveMigration()
        .allowMainThreadQueries() //TODO: ATTENTION allowed queries on main thread
        .build()

    private class ChatDatabaseCallback(): RoomDatabase.Callback(){
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Thread(Runnable{
                val db = get()
                db.insertUser("Gianni")
                db.insertUser("admin")
            }).start()
        }
    }


    // reference to a serverAdapter instance, one single instance accessed from a NotTalkRepository object
    private val server: ServerAdapter = ServerAdapter()

    // reference to an UserDao instance
    private val userDao = database.userDao()
    // reference to a UserRelationDao instance
    private val userRelationDao = database.userRelationDao()
    // reference to a MessageDao instance
    private val messageDao = database.messageDao()

    // single thread executor to perform functions as insert in database which needs not to stop the main (UI) thread
    private val executor = Executors.newSingleThreadExecutor()

    private val sharedPreferences = context.getSharedPreferences("notTalkPref", Service.MODE_PRIVATE)


/*------------------------------------------------------------------------------------ UserDao adapter functions ------------------------------------------------------------------------------------*/

    //fun getAllUsers(): LiveData<List<User>> = userDao.all // liveData enables to notify an observer about changes in the list

    fun findByUsername(username: String) : User {
        return userDao.findByUsername(username)
    }

    fun findIconByUsername(username: String): Bitmap{
        val bArray = userDao.findIconByUsername(username)
        return BitmapFactory.decodeByteArray(bArray, 0, bArray.size)
    }

    fun insertUser(username: String) {
        executor.execute {
            val b = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(b)
            val r = Random()
            val hsv = FloatArray(3)
            hsv[0]=r.nextInt(360).toFloat()
            hsv[1]= 0.70F
            hsv[2]= 0.70F
            val color = Color.HSVToColor(hsv)
            canvas.drawColor(color)
            val paint = Paint()
            paint.color= 0xFFFFFFFF.toInt()
            paint.textSize= 140F
            paint.textAlign=Paint.Align.CENTER
            canvas.drawText(username[0].toString().toUpperCase(), 100F, 150F, paint)
            val bos = ByteArrayOutputStream()
            b.compress(Bitmap.CompressFormat.PNG,100,bos)
            userDao.insert(User(username,bos.toByteArray()))
            bos.close()
        }
    }

    fun checkUser(username: String): Boolean{
        return userDao.doesExist(username)
    }

    fun deleteUser(username: String){
        executor.execute {
            userDao.deleteUser(username)
        }
    }

/*------------------------------------------------------------------------------------ UserRelationDao adapter functions ------------------------------------------------------------------------------------*/

    fun createRelation(otherUsername: String){
        executor.execute {
            val thisUsername = sharedPreferences.getString("thisUsername","")?:""
            if(thisUsername!=""){
                userRelationDao.insert(UserRelation(thisUsername,otherUsername))
            }
        }
    }

    fun insertRelation(ur: UserRelation){
        executor.execute {
            userRelationDao.insert(ur)
        }
    }

    fun removeRelation(otherUsername: String){
        executor.execute {
            val thisUsername = sharedPreferences.getString("thisUsername","")?:""
            if(thisUsername!=""){
                userRelationDao.delete(thisUsername,otherUsername)
            }
        }
    }

    fun existsRelation(thisUsername: String, otherUsername: String): Boolean {
        return userRelationDao.existsRelation(thisUsername,otherUsername)
    }

    fun getAllUsers(username: String): LiveData<List<User>> {
        return userRelationDao.get(username)
    }

    fun getByUsers(thisUser: String, otherUser: String): UserRelation? {
        return userRelationDao.getByUsers(thisUser, otherUser)
    }

    fun getById(id: Int): UserRelation? {
        return userRelationDao.getById(id)
    }

    fun deleteRelationsByThisUser(username: String){
        executor.execute {
            userRelationDao.deleteAllByThisUser(username)
        }
    }

    fun deleteRelationsByOtherUser(username: String){
        executor.execute {
            userRelationDao.deleteAllByOtherUser(username)
        }
    }

/*------------------------------------------------------------------------------------ MessageDao adapter functions ------------------------------------------------------------------------------------*/

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

    fun getConvo(thisUser: String, otherUser: String): LiveData<List<Message>> = messageDao.findConvo(thisUser, otherUser)

    fun getConvoNotLiveData(thisUser: String, otherUser: String): List<Message> = messageDao.findConvoNonLiveData(thisUser, otherUser)

    fun setAsRead(toUser: String,fromUser: String){
        executor.execute {
            messageDao.setAsRead(toUser, fromUser)
        }
    }

    fun getUnreadCount(toUser: String,fromUser: String): LiveData<Int>{
        return messageDao.getUnreadCount(toUser,fromUser)
    }

    fun getLast(toUser: String,fromUser: String): LiveData<Message>{
        return messageDao.getLast(toUser,fromUser)
    }

    // to delete a message from the database
    fun deleteMessage(id: Long){
        executor.execute {
            messageDao.deleteById(id)
        }
    }

    fun deleteByUserTo(toUser: String){
        executor.execute {
            messageDao.deleteByUserTo(toUser)
        }
    }

    fun deleteByUserFrom(fromUser: String){
        executor.execute {
            messageDao.deleteByUserFrom(fromUser)
        }
    }


/*------------------------------------------------------------------------------------ Server adapter functions ------------------------------------------------------------------------------------*/

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
            var id: Long = -1
            executor.execute {
                id = messageDao.insert(msg)
            }
            // tries to send it to the server
            var response: String = ""
            try {
                response = server.sendTextMsg(msg.fromUser, uuid, msg.toUser, msg.date, msg.text)
            }
            catch(ex: Exception){
                println(ex.message)
                ex.printStackTrace()
                context.mainExecutor.execute {
                    Toast.makeText(context,"Offline", Toast.LENGTH_LONG).show()
                }
            }
            if (response != "ok") {
                //Toast.makeText(context.applicationContext, response, Toast.LENGTH_SHORT).show() // creates toast displaying error
                    Log.d("Server error", uuid)
                Log.d("Server error", response)
                deleteMessage(id) // deletes it from local database if couldn't send it
                context.mainExecutor.execute {
                    Toast.makeText(context,"Error occurred, please re-login", Toast.LENGTH_LONG).show()
                }
            }
        }).start()
    }

    fun sendFileMessage(context: Context, uri: Uri, thisUsername: String, uuid: String, otherUsername: String){
        Thread(Runnable {
            val contentResolver = context.contentResolver
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val mimetype = contentResolver.getType(uri)
            val filename = DocumentFile.fromSingleUri(context, uri)?.name
            val fileInStream: InputStream
            val date = Calendar.getInstance().timeInMillis

            contentResolver.run {
                fileInStream = openInputStream(uri) ?: throw Exception("Error file")
            }
            val content = Base64.encodeToString(fileInStream.readBytes(), Base64.DEFAULT)
            val msg = Message(otherUsername, thisUsername, date, "file", uri.toString())
            msg.fileName = filename!!
            msg.mimeType = mimetype!!
            var id: Long = -1
            executor.execute {
                id = messageDao.insert(msg)
            }

            var result: String = ""
            try {
                result = server.sendFileMsg(
                    thisUsername,
                    uuid,
                    otherUsername,
                    date,
                    content,
                    mimetype!!,
                    filename!!
                )
            }
            catch(ex: Exception){
                println(ex.message)
                ex.printStackTrace()
                context.mainExecutor.execute {
                    Toast.makeText(context,"Offline", Toast.LENGTH_LONG).show()
                }
            }
            if (result != "ok") {
                deleteMessage(id)
                context.mainExecutor.execute {
                    Toast.makeText(context,"Error occurred, please re-login", Toast.LENGTH_LONG).show()
                }
            }
        }).start()
    }




    // Singleton Design Pattern for this class
    companion object{
        @SuppressLint("StaticFieldLeak")
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