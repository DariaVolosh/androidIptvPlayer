package com.example.iptvplayer.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "epg")
data class Epg(
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0,

    @ColumnInfo(name = "channel_id")
    var channelId: String = "",

    @ColumnInfo(name = "program_start")
    var programStart: String = "",

    @ColumnInfo(name = "program_end")
    var programEnd: String = "",

    @ColumnInfo(name = "program_name")
    var programName: String = ""
)