package it.unipd.dei.esp2021.nottalk.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message")
data class Message(@ColumnInfo(name = "username") val username: String,
                   @ColumnInfo(name = "date") val date: String,
                   @ColumnInfo(name = "type") val type: String,
                   @ColumnInfo(name = "text") val text: String){
    @PrimaryKey(autoGenerate = true) var id: Int? = null
}