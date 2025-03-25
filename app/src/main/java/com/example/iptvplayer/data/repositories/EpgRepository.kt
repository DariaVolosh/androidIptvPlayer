package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.Utils
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.TimeZone
import javax.inject.Inject

class EpgRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /*suspend fun getCountryCodeById(id: String): String {
        val code = CompletableDeferred<String>()
        Log.i("get country code by id", id)
        Log.i("get country code by id", channelsIdToEpgIdMapper[id].toString())

        firestore
            .collection("channel_id_to_country_mapping_prod").document("mapping")
            .get()
            .addOnSuccessListener { result ->
                Log.i("onSuccessLmao", channelsIdToEpgIdMapper[id].toString())
                Log.i("onSuccessLmao", result.data.toString())
                result.data?.let { data ->
                    code.complete(data[id].toString())
                }
            }

        return code.await()
    } */

    fun getEpgYearRef(
        channelId: String
    ): DocumentReference {
        Log.i("epg channel id", channelId)

        return firestore
            //.collection("epg_prod").document(countryCode)
            .collection("epg_prod").document(channelId)
            .collection("years").document("2025")
    }

    fun getEpgMonthsRef(channelId: String): CollectionReference {
        return getEpgYearRef(channelId).collection("months")
    }

    fun getEpgDaysRef(channelId: String, month: String): CollectionReference {
        return getEpgMonthsRef(channelId)
            .document(month).collection("days")
    }

    suspend fun getEpgMonth(
        channelId: String
    ): Int {
        Log.i("get epg month", channelId)
        val epgMonth = getEpgMonthsRef(channelId).get().await()
        return if (epgMonth.documents.size == 0) -1 else epgMonth.documents[0].id.toInt()
    }


    suspend fun getEpgFirstAndLastDays(
        channelId: String,
        month: String
    ): Pair<Int,Int> {
        val daysRef = getEpgDaysRef(channelId, month).get().await()
        Log.i("epg ref exists", daysRef.documents.size.toString())

        val sortedEpgDays = daysRef.documents.sortedBy { doc -> doc.id.toInt() }
        if (sortedEpgDays.isEmpty()) return Pair(-1, -1)
        val minimumDay = sortedEpgDays[0].id.toInt()
        val maximumDay = sortedEpgDays[sortedEpgDays.size-1].id.toInt()

        val firstAndLastDay = Pair(minimumDay, maximumDay)
        Log.i("FIRST AND LAST DAY", firstAndLastDay.toString())
        return firstAndLastDay
    }

    suspend fun getFirstAndLastEpgTimestamps(
        channelId: String,
        month: String
    ): Pair<Long, Long> {
        val epgFirstAndLastDays = getEpgFirstAndLastDays(channelId, month)
        if (epgFirstAndLastDays.first == -1) return Pair(-1,-1)


        val firstDayEpgRef = getEpgDaysRef(channelId, month)
            .document(epgFirstAndLastDays.first.toString())
            .collection("programmes_list")

        val lastDayEpgRef = getEpgDaysRef(channelId, month)
            .document(epgFirstAndLastDays.second.toString())
            .collection("programmes_list")

        val firstEpgTimestamp = getFirstEpgTimestampOfDay(firstDayEpgRef)
        val lastEpgTimestamp = getLastEpgTimestampOfDay(lastDayEpgRef)

        return Pair(firstEpgTimestamp, lastEpgTimestamp)
    }

    suspend fun getFirstEpgTimestampOfDay(dayRef: CollectionReference): Long {
        val firstEpgTimestamp = dayRef.orderBy("start_time").get().await()
        return firstEpgTimestamp.documents[0].id.toLong()
    }

    suspend fun getLastEpgTimestampOfDay(dayRef: CollectionReference): Long {
        val lastEpgTimestamp = dayRef.orderBy("start_time").get().await()
        return lastEpgTimestamp.documents[lastEpgTimestamp.documents.size-1].id.toLong()
    }

    suspend fun getDayEpg(
        channelId: String,
        month: String,
        day: String,
        dvrRange: Pair<Long, Long>
    ): MutableList<Epg> {
        Log.i("Thread", "Before Firestore: ${Thread.currentThread().name}")

        val epgList = mutableListOf<Epg>()

        val currentDayGmtProgrammesList =
            getEpgDaysRef(channelId, month)
                .document(day)
                .collection("programmes_list").get().await()

        Log.i("current gmt epg", currentDayGmtProgrammesList.documents.toString())

        for (epgDoc in currentDayGmtProgrammesList.documents) {
            Log.i("epg title", epgDoc["title"].toString())
            Log.i("epg title","epg day $day")
            Log.i("epg title", "epg stop time ${epgDoc["stop_time"]}")
            val startTime = epgDoc["start_time"].toString().toLong()
            val stopTime = epgDoc["stop_time"].toString().toLong()
            val duration = stopTime - startTime
            val title = epgDoc["title"].toString()

            var isDvrAvailable = false

            if (
                dvrRange.first > 0 &&
                dvrRange.first <= startTime &&
                (dvrRange.second >= stopTime || startTime < Utils.getGmtTime())
            ) {
                isDvrAvailable = true
            }

            val epg = Epg(startTime, stopTime, duration, title, isDvrAvailable)
            epgList += epg
            Log.i("WHAT", "${epg.toString()} $day")
        }

        Log.i("Thread", "after Firestore: ${Thread.currentThread().name}")
        Log.i("WHAT", "executed")

        return epgList.sortedBy { epg -> epg.startTime }.toMutableList()
    }

    fun getEpgById(channelId: String, dvrRange: Pair<Long, Long>) = flow {
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

        val firstAndLastEpgDay = getEpgFirstAndLastDays(channelId, currentGmtMonth.toString())
        val firstEpgDay = firstAndLastEpgDay.first
        val lastEpgDay = firstAndLastEpgDay.second

        Log.i("first and last epg day", "$firstEpgDay $lastEpgDay")

        if (firstEpgDay == -1) return@flow

        var dayCount = 0
        while (!allNextDaysFetched || !allPreviousDaysFetched) {
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
                    if (previousGmtDay < firstEpgDay) {
                        allPreviousDaysFetched = true
                        isPreviousDayEpgFetched = !isPreviousDayEpgFetched
                    }

                    // dummy epg that indicates that all the epg of the previous day are being fetched
                    dummyEpg = Epg(-2,-2,-2,"")
                } else {
                    localGmtDay = nextGmtDay
                    nextGmtDay += 1

                    if (nextGmtDay > lastEpgDay) {
                        allNextDaysFetched = true
                        isPreviousDayEpgFetched = !isPreviousDayEpgFetched
                    }

                    // dummy epg that indicates that all the epg of the next day are being fetched
                    dummyEpg = Epg(-3,-3,-3,"")
                }

                if (!allNextDaysFetched && !allPreviousDaysFetched) {
                    isPreviousDayEpgFetched = !isPreviousDayEpgFetched
                }
            }

            val startTimeSortedEpg = getDayEpg(
                channelId,
                currentGmtMonth.toString(),
                localGmtDay.toString(),
                dvrRange
            )

            startTimeSortedEpg.add(0, dummyEpg)
            Log.i("WHY", "${startTimeSortedEpg}")
            Log.i("WHY", localGmtDay.toString())
            if (startTimeSortedEpg.size > 1) emit(startTimeSortedEpg)
            dayCount++
        }
    }
}