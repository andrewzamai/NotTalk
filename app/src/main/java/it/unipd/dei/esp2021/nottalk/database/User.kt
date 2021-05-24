package it.unipd.dei.esp2021.nottalk.database

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user" , indices = [Index(value = ["username"], unique = true)] )
data class User(@ColumnInfo(name = "username") val username: String,
                @ColumnInfo(name = "picture") var picture: ByteArray? = null){
    @PrimaryKey(autoGenerate = true) var id: Int? = null
}

