package com.example.iptvplayer.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Channel::class, Epg::class], version = 2)
abstract class AppDatabase: RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao
}