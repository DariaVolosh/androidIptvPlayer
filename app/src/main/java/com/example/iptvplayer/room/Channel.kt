package com.example.iptvplayer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey
    @ColumnInfo(name="channel_name")
    var channelName: String = "",

    @ColumnInfo(name = "channel_id")
    var channelId: String = ""
)

