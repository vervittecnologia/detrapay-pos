package com.detrapay.data.database

import androidx.room.TypeConverter
import com.detrapay.data.model.Company
import com.detrapay.data.model.Dispatcher
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

    @TypeConverter
    fun fromCompanyList(value: String?): List<Company>? {
        if (value == null) {
            return null
        }
        val listType: Type = object : TypeToken<List<Company?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toCompanyList(list: List<Company?>?): String? {
        if (list == null) {
            return null
        }
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromDispatcherList(value: String?): List<Dispatcher>? {
        if (value == null) {
            return null
        }
        val listType: Type = object : TypeToken<List<Dispatcher?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toDispatcherList(list: List<Dispatcher?>?): String? {
        if (list == null) {
            return null
        }
        val gson = Gson()
        return gson.toJson(list)
    }
}