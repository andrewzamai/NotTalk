package it.unipd.dei.esp2021.nottalk.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "userRelation", indices = [Index(value = ["thisUser", "otherUser"], unique = true)])
data class UserRelation(
    @ColumnInfo(name="thisUser") val thisUser: String,
    @ColumnInfo(name="otherUser") val otherUser: String){
    @PrimaryKey(autoGenerate=true) var id: Int? = null
}
