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

        var allPreviousDaysFetched = false
        var allNextDaysFetched = false

        val epgAvailableDays = firestore
            .collection("epg_prod").document(countryCode)
            .collection("channels_id").document(channelId)
            .collection("years").document("2025")
            .collection("months").document("$currentGmtMonth")
            .collection("days").get().await()

        if (epgAvailableDays.documents.size == 0) return@flow

        val sortedEpgDays = epgAvailableDays.documents.sortedBy { doc -> doc.id.toInt() }
        val minimumDay = sortedEpgDays[0].id.toInt()
        val maximumDay = sortedEpgDays[sortedEpgDays.size-1].id.toInt()

        Log.i("MINIMUM EPG DAY", minimumDay.toString())
        Log.i("MAXIMUM EPG DAY", maximumDay.toString())

        var dayCount = 0
        while (dayCount < 10) {
            val epgList = mutableListOf<Epg>()
            var localGmtDay: Int

            // dummy epg to indicate that all the epgs of the day are fetched
            // this is needed to calculate the positions of the epg programmes in overall list
            // of epg in UI
            var dummyEpg: Epg

            if (!isCurrentDayEpgFetched) {
                localGmtDay = currentGmtDay
                isCurrentDayEpgFetched = true

                // dummy epg that indicates that all the epg of the current day are being fetched
                dummyEpg = Epg(-1,-1,-1,"")
            } else {
                if (!isPreviousDayEpgFetched && !allPreviousDaysFetched) {
                    localGmtDay = previousGmtDay
                    previousGmtDay -= 1
                    if (previousGmtDay < minimumDay) allPreviousDaysFetched = true

                    // dummy epg that indicates that all the epg of the previous day are being fetched
                    dummyEpg = Epg(-2,-2,-2,"")
                } else {
                    localGmtDay = nextGmtDay
                    nextGmtDay += 1

                    // dummy epg that indicates that all the epg of the next day are being fetched
                    dummyEpg = Epg(-3,-3,-3,"")
                }

                isPreviousDayEpgFetched = !isPreviousDayEpgFetched
            }

            Log.i("CURRENT GMT DAY", localGmtDay.toString())
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
            startTimeSortedEpg.add(0, dummyEpg)
            Log.i("WHY", startTimeSortedEpg.toString())
            Log.i("WHY", localGmtDay.toString())
            emit(startTimeSortedEpg)
            dayCount++
        }
    }
}