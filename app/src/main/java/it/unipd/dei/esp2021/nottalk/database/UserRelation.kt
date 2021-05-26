package it.unipd.dei.esp2021.nottalk.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "userRelation", primaryKeys = ["thisUser","otherUser"])
data class UserRelation(
    @ColumnInfo(name="thisUser") val thisUser: String,
    @ColumnInfo(name="otherUser") val otherUser: String
)
