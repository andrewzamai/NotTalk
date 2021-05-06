package it.unipd.dei.esp2021.nottalk.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(entities = [(User::class), (Message::class)], version = 1, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        // TODO: delete getDataBase function an retrieve a ChatDatabase instance via NotTalkRepository get function
        fun getDatabase(context: Context): ChatDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        ChatDatabase::class.java,
                        "chat_database"
                )
                    .addCallback(ChatDatabaseCallback())
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }

        // populate database (comment out if doesn't work)

        private class ChatDatabaseCallback(): Callback(){
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Thread{
                    val messageDao = INSTANCE?.messageDao()
                    val userDao = INSTANCE?.userDao()
                    userDao?.insert(User("Tizio"))
                    messageDao?.insert(Message("Tizio", "Caio", 12345678, "text", "ciao"))
                }.run()
            }
        }



        fun destroyInstance() {
            INSTANCE = null
        }
    }
}