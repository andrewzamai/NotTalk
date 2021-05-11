package it.unipd.dei.esp2021.nottalk.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message")
data class Message(@ColumnInfo(name = "touser") val toUser: String,
                   @ColumnInfo(name = "fromuser") val fromUser: String,
                   @ColumnInfo(name = "date") val date: Long,
                   @ColumnInfo(name = "type") val type: String,
                   @ColumnInfo(name = "text") var text: String){
    @PrimaryKey(autoGenerate = true) var id: Int? = null
    @ColumnInfo(name="filename") var fileName: String? = null
    @ColumnInfo(name="mimetype") var mimeType: String? = null
}