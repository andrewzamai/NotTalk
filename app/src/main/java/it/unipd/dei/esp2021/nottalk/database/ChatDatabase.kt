package it.unipd.dei.esp2021.nottalk.database

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [(User::class), (Message::class), (UserRelation::class)], version = 5, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userRelationDao(): UserRelationDao
    abstract fun messageDao(): MessageDao

    /*
    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        // retrieve a ChatDatabase instance via NotTalkRepository get function
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
                Thread(Runnable{
                    val messageDao = INSTANCE?.messageDao()
                    val userDao = INSTANCE?.userDao()
                    userDao?.insert(User("Gianni"))
                    userDao?.insert(User("admin"))
                    //messageDao?.insert(Message("Tizio", "Caio", 12345678, "text", "ciao"))
                }).start()
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }*/
}