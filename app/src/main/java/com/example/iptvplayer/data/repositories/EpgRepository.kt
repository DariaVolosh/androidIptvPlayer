package com.example.iptvplayer.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.iptvplayer.retrofit.data.EpgListItem
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EpgRepository @Inject constructor(
    private val channelsAndEpgService: ChannelsAndEpgService,
    private val dataStore: DataStore<Preferences>
) {
    private val gson = Gson()
    private object EpgListType : TypeToken<List<EpgListItem.Epg>>()

    // VERSION NEW (IDK IF IT WILL WORKS NORMALLY LMAO)
   suspend fun getEpgById(
        requestedEpgId: Int
   ): List<EpgListItem.Epg> =
       withContext(Dispatchers.IO) {
           val response = channelsAndEpgService.getEpgForChannel(requestedEpgId)
           response.data.data
       }

    suspend fun saveEpgToCache(
        epgId: Int,
        epgList: List<EpgListItem.Epg>
    ) {
        dataStore.edit { preferences ->
            val json = gson.toJson(epgList)
            preferences[stringPreferencesKey("epg_id_$epgId")] = json
        }
    }

    suspend fun getCachedEpg(
        epgId: Int
    ): List<EpgListItem.Epg> =
        dataStore.data.first()[stringPreferencesKey("epg_id_$epgId")]?.let { json ->
            gson.fromJson(json, TypeToken.getParameterized(List::class.java, EpgListItem.Epg::class.java).type) ?: emptyList()
        } ?: emptyList()

    suspend fun isEpgCached(
        epgId: Int
    ): Boolean =
        dataStore.data.first().contains(stringPreferencesKey("epg_id_$epgId"))
}