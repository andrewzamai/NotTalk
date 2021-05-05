package it.unipd.dei.esp2021.nottalk

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import it.unipd.dei.esp2021.nottalk.database.ChatDatabase
import it.unipd.dei.esp2021.nottalk.database.User
import java.lang.IllegalStateException
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
    ).build()

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

    // MessageDao adapter functions




    // Singleton Design Pattern
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