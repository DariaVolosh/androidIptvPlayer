package com.example.iptvplayer.data.repositories

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.Utils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.TimeZone
import javax.inject.Inject

class EpgRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getCountryCodeById(id: String): String {
        val code = CompletableDeferred<String>()

        firestore
            .collection("channel_id_to_country_mapping_prod").document("mapping")
            .get()
            .addOnSuccessListener { result ->
                Log.i("onSuccessLmao", result[id].toString())
                code.complete(result[id].toString())
            }

        return code.await()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEpgById(channelId: String, countryCode: String) = flow {
        val datePattern = "yyyy d M HH:mm:ss"

        val currentGmtTime = Utils.getGmtTime()
        val currentGmtCalendar = Utils.getCalendar(currentGmtTime, TimeZone.getTimeZone("Z"))
        val currentGmtDay = Utils.getCalendarDay(currentGmtCalendar)
        val currentGmtMonth = Utils.getCalendarMonth(currentGmtCalendar) + 1

        var previousGmtDay = currentGmtDay - 1
        var nextGmtDay = currentGmtDay + 1
        var isCurrentDayEpgFetched = false
        var isPreviousDayEpgFetched = false

        var i = 0
        while (i < 10) {
            val epgList = mutableListOf<Epg>()
            var localGmtDay: Int

            if (!isCurrentDayEpgFetched) {
                localGmtDay = currentGmtDay
                isCurrentDayEpgFetched = true
            } else {
                if (!isPreviousDayEpgFetched) {
                    localGmtDay = previousGmtDay
                    previousGmtDay -= 1
                } else {
                    localGmtDay = nextGmtDay
                    nextGmtDay += 1
                }

                isPreviousDayEpgFetched = !isPreviousDayEpgFetched
            }

            Log.i("Thread", "Before Firestore: ${Thread.currentThread().name}")
            val currentDayGmtProgrammesList = firestore
                .collection("epg_prod").document(countryCode)
                .collection("channels_id").document(channelId)
                .collection("years").document("2025")
                .collection("months").document("$currentGmtMonth")
                .collection("days").document("$localGmtDay")
                .collection("programmes_list").get().await()

            for (epgDoc in currentDayGmtProgrammesList.documents) {
                val startTime = epgDoc["start_time"].toString().toLong()
                val stopTime = epgDoc["stop_time"].toString().toLong()
                val duration = stopTime - startTime
                val title = epgDoc["title"].toString()

                val epg = Epg(startTime, stopTime, duration, title)
                epgList += epg
                Log.i("WHAT", "${epg.toString()} $localGmtDay")
            }

            Log.i("Thread", "after Firestore: ${Thread.currentThread().name}")
            Log.i("WHAT", "executed")

            val startTimeSortedEpg: MutableList<Epg> = epgList.sortedBy { epg -> epg.startTime }.toMutableList()
            // dummy epg to indicate that all the epgs of the current day are fetched
            startTimeSortedEpg.add(Epg(0,0,0,""))
            Log.i("WHY", startTimeSortedEpg.toString())
            Log.i("WHY", localGmtDay.toString())
            startTimeSortedEpg.forEach {
                epg -> emit(epg)
                Log.i("EMITTED", "${epg} $localGmtDay")
            }
            i++
        }
    }
}