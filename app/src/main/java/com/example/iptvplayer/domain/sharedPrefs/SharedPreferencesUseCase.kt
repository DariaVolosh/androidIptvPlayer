package com.example.iptvplayer.domain.sharedPrefs

import com.example.iptvplayer.data.repositories.SharedPreferencesRepository
import javax.inject.Inject

class SharedPreferencesUseCase @Inject constructor(
    private val sharedPreferencesRepository: SharedPreferencesRepository
) {
    fun getIntValue(key: String) = sharedPreferencesRepository.getIntValue(key)
    fun saveIntValue(key: String, value: Int) = sharedPreferencesRepository.saveIntValue(key, value)

    fun <T> getObject(key: String, c: Class<T>) = sharedPreferencesRepository.getObject(key, c)
    fun saveObject(key: String, obj: Any) = sharedPreferencesRepository.saveObject(key, obj)

    fun getLongValue(key: String) = sharedPreferencesRepository.getLongValue(key)
    fun saveLongValue(key: String, value: Long) = sharedPreferencesRepository.saveLongValue(key, value)

    fun getBooleanValue(key: String) = sharedPreferencesRepository.getBooleanValue(key)
    fun saveBooleanValue(key: String, value: Boolean) = sharedPreferencesRepository.saveBooleanValue(key, value)
}