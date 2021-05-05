package it.unipd.dei.esp2021.nottalk.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @get:Query("SELECT * FROM user")
    val all: LiveData<List<User>> //returns a List of User elements in a LiveData object

    @Query("SELECT * FROM user WHERE id IN (:userIds)")
    fun loadAllByIds(userIds: Array<Int>): LiveData<List<User>>

    @Query("SELECT * FROM user WHERE id = :id")
    fun findByCustomerId(id: Int): LiveData<List<User>>

    @Query("SELECT * FROM user WHERE username = :username")
    fun findByUsername(username: String): LiveData<List<User>>

    @Insert
    fun insertAll(bills: List<User>)

    @Insert
    fun insert(bill: User)

    @Delete
    fun delete(bill: User)
}