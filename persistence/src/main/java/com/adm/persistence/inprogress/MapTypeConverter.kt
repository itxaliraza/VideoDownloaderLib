package com.adm.persistence.inprogress

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MapTypeConverter {
    private val gson = Gson()
    @TypeConverter
    fun fromMapToString(map: Map<String, String>?): String? {
        return gson.toJson(map)
    }

    @TypeConverter
    fun fromStringToMap(value: String?): Map<String, String>? {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type)
    }
}