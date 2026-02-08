package com.detrapay.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.detrapay.data.database.DbConstant

@Entity(tableName = DbConstant.USER_TABLE)
data class User(
    @PrimaryKey
    val id: Int,
    var token: String,
    var name: String,
    var email: String,
    var username: String,
)
