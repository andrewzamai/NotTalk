package it.unipd.dei.esp2021.nottalk.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @get:Query("SELECT * FROM message ORDER BY date DESC")
    val all: LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE toUser = :toUser and fromUser = :username ORDER BY date DESC")
    fun findSentTo(toUser: String,username: String): LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE toUser = :username and fromUser = :fromUser ORDER BY date DESC")
    fun findRecivedFrom(fromUser: String,username: String): LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE (toUser = :user1 and fromUser = :user2) or (toUser = :user2 and fromUser = :user1) ORDER BY date DESC")
    fun findConvo(user1: String, user2: String): LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE id IN (:messageIds)")
    fun findAllByIds(messageIds: Array<Int>): LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE id = :id")
    fun findById(id: Int): LiveData<List<Message>>

    @Insert
    fun insertAll(bills: List<Message>)

    @Insert
    fun insert(bill: Message)

    @Delete
    fun delete(bill: Message)
}