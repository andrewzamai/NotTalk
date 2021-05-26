package it.unipd.dei.esp2021.nottalk.database

import androidx.lifecycle.LiveData
import androidx.room.*

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

    @Query("SELECT EXISTS(SELECT * FROM user WHERE username = :username)")
    fun doesExist(username : String) : Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(bills: List<User>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(bill: User)

    @Query("DELETE FROM user WHERE username = :username")
    fun deleteUser(username: String)

    @Delete
    fun delete(bill: User)
}