package com.detrapay.data.database

import androidx.room.TypeConverter
import com.detrapay.data.model.Company
import com.detrapay.data.model.Dispatcher
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromCompanyList(value: List<Company>): String {
        val gson = Gson()
        val type = object : TypeToken<List<Company>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toCompanyList(value: String): List<Company> {
        val gson = Gson()
        val type = object : TypeToken<List<Company>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromDispatcherList(value: List<Dispatcher>): String {
        val gson = Gson()
        val type = object : TypeToken<List<Dispatcher>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toDispatcherList(value: String): List<Dispatcher> {
        val gson = Gson()
        val type = object : TypeToken<List<Dispatcher>>() {}.type
        return gson.fromJson(value, type)
    }
}
