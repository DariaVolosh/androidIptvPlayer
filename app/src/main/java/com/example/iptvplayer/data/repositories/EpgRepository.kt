package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.room.Channel
import com.example.iptvplayer.room.ChannelDao
import com.example.iptvplayer.room.Epg
import com.example.iptvplayer.room.EpgDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import javax.inject.Inject

class EpgRepository @Inject constructor(
    private val channelDao: ChannelDao,
    private val epgDao: EpgDao,
    private val xmlPullParser: XmlPullParser
) {
    suspend fun parseEpgChannelsData(inputStream: InputStream, channelNames: Set<String>) {
        withContext(Dispatchers.IO) {
            xmlPullParser.setInput(inputStream, "UTF-8")

            var eventType = xmlPullParser.eventType
            var tagName = ""
            var channel = Channel()
            var epg = Epg()

            while (xmlPullParser.eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        tagName = xmlPullParser.name

                        if (tagName == "channel") {
                            channel.channelId = xmlPullParser.getAttributeValue(null, "id")

                        } else if (tagName == "programme") {
                            val channelId = xmlPullParser.getAttributeValue(null, "channel")
                            if (channelDao.idExists(channelId)) {
                                val programStart = xmlPullParser.getAttributeValue(null, "start")
                                val programEnd = xmlPullParser.getAttributeValue(null, "stop")

                                epg.programStart = programStart
                                epg.programEnd = programEnd
                                epg.channelId = channelId
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (tagName == "display-name") {
                            val channelName = xmlPullParser.text

                            if (channelNames.contains(channelName)) {
                                channel.channelName = channelName
                                try {
                                    channelDao.insertChannel(channel)
                                } catch (e: Exception) {

                                }

                                Log.i("insertChannel", channel.toString())

                                channel = Channel("", "")
                            }
                        } else if (tagName == "title") {
                            val programmeTitle = xmlPullParser.text

                            if (epg.channelId != "") {
                                epg.programName = programmeTitle
                                // comment out to prevent fetching epg again for now
                                //epgDao.insertEpg(epg)
                                Log.i("insertEpg", epg.toString())
                                epg = Epg()
                            }
                        }
                    }
                }

                eventType = xmlPullParser.next()
            }
        }
    }

    suspend fun fetchChannelEpg(channelName: String): List<Epg> =
        withContext(Dispatchers.IO) {
            val channelId = channelDao.getChannelIdByName(channelName)
            Log.i("EPG LOL", "$channelId shit")
            val epg = epgDao.getCurrentChannelEpg(channelId)
            Log.i("EPG LOL", epg.toString())
            epg
        }
}