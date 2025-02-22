package com.example.iptvplayer.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg")
    fun getEpg(): List<Epg>

    @Query("SELECT * FROM epg WHERE channel_id = :channelId")
    fun getCurrentChannelEpg(channelId: String): List<Epg>

    @Insert
    fun insertEpg(epg: Epg)
}