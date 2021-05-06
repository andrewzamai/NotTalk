package it.unipd.dei.esp2021.nottalk.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @get:Query("SELECT * FROM message")
    val all: List<Message>

    @Query("SELECT * FROM message WHERE toUser = :toUser and fromUser = :username")
    fun findSentTo(toUser: String,username: String): List<Message>

    @Query("SELECT * FROM message WHERE toUser = :username and fromUser = :fromUser")
    fun findRecivedFrom(fromUser: String,username: String): List<Message>

    @Query("SELECT * FROM message WHERE (toUser = :user1 and fromUser = :user2) or (toUser = :user2 and fromUser = :user1)")
    fun findConvo(user1: String,user2: String): List<Message>

    @Query("SELECT * FROM message WHERE id IN (:messageIds)")
    fun findAllByIds(messageIds: Array<Int>): List<Message>

    @Query("SELECT * FROM message WHERE id = :id")
    fun findById(id: Int): List<Message>

    @Insert
    fun insertAll(bills: List<Message>)

    @Insert
    fun insert(bill: Message)

    @Delete
    fun delete(bill: Message)
}