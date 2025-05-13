package com.detrapay.data.database

import androidx.room.TypeConverter
import com.detrapay.data.model.local.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class DataConverter {
    @TypeConverter
    fun fromUsersList(user: List<User?>?): String? {
        if (user == null) {
            return null
        }
        val gson = Gson()
        val type: Type = object : TypeToken<List<User?>?>() {}.type
        return gson.toJson(user, type)
    }

    @TypeConverter
    fun toUsersList(countryLangString: String?): List<User>? {
        if (countryLangString == null) {
            return null
        }
        val gson = Gson()
        val type: Type = object : TypeToken<List<User?>?>() {}.type
        return gson.fromJson(countryLangString, type)
    }
}