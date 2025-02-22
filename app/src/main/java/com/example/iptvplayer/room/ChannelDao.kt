package com.example.iptvplayer.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChannelDao {
    @Query("SELECT channel_id FROM channels WHERE channel_name = :channelName")
    fun getChannelIdByName(channelName: String): String

    @Query("SELECT EXISTS(SELECT 1 FROM channels WHERE channel_id = :id)")
    fun idExists(id: String): Boolean

    @Insert
    fun insertChannel(channel: Channel)

    @Query("SELECT * FROM channels")
    fun getChannels(): List<Channel>
}