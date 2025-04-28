package com.example.iptvplayer.data.repositories

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import javax.inject.Inject

class SharedPreferencesRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {

    fun getIntValue(key: String): Int {
        val value = sharedPreferences.getInt(key, -1)
        Log.i("get int value key", "$key")
        Log.i("get int value key", "$value")
        return value
    }
    fun saveIntValue(key: String, value: Int) =
        sharedPreferences.edit()
        .putInt(key, value)
        .apply()

    fun <T> getObject(key: String, c: Class<T>): T {
        val jsonString = sharedPreferences.getString(key, "")
        val gson = Gson()
        return gson.fromJson(jsonString, c)
    }

    fun saveObject(key: String, obj: Any) {
        val gson = Gson()
        val jsonString = gson.toJson(obj)
        sharedPreferences.edit()
            .putString(key, jsonString)
            .apply()
    }

    fun getLongValue(key: String) = sharedPreferences.getLong(key, 0)
    fun saveLongValue(key: String, value: Long) =
        sharedPreferences.edit()
            .putLong(key, value)
            .apply()

    fun getBooleanValue(key: String) = sharedPreferences.getBoolean(key, true)
    fun saveBooleanValue(key: String, value: Boolean) =
        sharedPreferences.edit()
            .putBoolean(key, value)
            .apply()
}