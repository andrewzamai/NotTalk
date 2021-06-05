package it.unipd.dei.esp2021.nottalk.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UserRelationDao {
    @Query("SELECT * FROM user WHERE username IN (SELECT otherUser FROM userRelation WHERE thisUser = :thisUser)")
    fun get(thisUser: String): LiveData<List<User>>

    @Query("SELECT * FROM userRelation WHERE id = :id")
    fun getById(id: Int): UserRelation?

    @Query("SELECT * FROM userRelation WHERE thisUser = :thisUser AND otherUser = :otherUser")
    fun getByUsers(thisUser: String,otherUser: String): UserRelation?


    @Query("SELECT otherUser FROM userRelation WHERE thisUser = :thisUser")
    fun getAllOtherUserByUsers(thisUser: String): LiveData<List<String>>


    @Query("WITH ur(user1,user2) AS (SELECT thisUser, otherUser FROM userRelation WHERE id = :id LIMIT 1) SELECT * FROM message WHERE (toUser = (SELECT user1 FROM ur)  and fromUser = (SELECT user2 FROM ur)) or (toUser = (SELECT user2 FROM ur) and fromUser = (SELECT user1 FROM ur)) ORDER BY date DESC")
    fun getConvoById(id: Int): LiveData<Message>

    @Query("SELECT EXISTS(SELECT * FROM userRelation WHERE thisUser = :thisUser AND otherUser = :otherUser)")
    fun existsRelation(thisUser: String, otherUser: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(bill: UserRelation): Long

    @Query("DELETE FROM userRelation WHERE thisUser = :thisUser")
    fun deleteAllByThisUser(thisUser: String)

    @Query("DELETE from userRelation WHERE otherUser = :otherUser")
    fun deleteAllByOtherUser(otherUser: String)

    @Query("DELETE FROM userRelation WHERE thisUser = :thisUser AND otherUser = :otherUser")
    fun delete(thisUser: String,otherUser: String)

    @Delete
    fun delete(bill: UserRelation)
}