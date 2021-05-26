package it.unipd.dei.esp2021.nottalk.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UserRelationDao {
    @Query("SELECT * FROM user WHERE username IN (SELECT otherUser FROM userRelation WHERE thisUser = :thisUser)")
    fun get(thisUser: String): LiveData<List<User>>

    @Query("SELECT EXISTS(SELECT * FROM userRelation WHERE thisUser = :thisUser AND otherUser = :otherUser)")
    fun existsRelation(thisUser: String, otherUser: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(bill: UserRelation)

    @Query("DELETE FROM userRelation WHERE thisUser = :thisUser")
    fun deleteAllByThisUser(thisUser: String)

    @Query("DELETE from userRelation WHERE otherUser = :otherUser")
    fun deleteAllByOtherUser(otherUser: String)

    @Delete
    fun delete(bill: UserRelation)
}