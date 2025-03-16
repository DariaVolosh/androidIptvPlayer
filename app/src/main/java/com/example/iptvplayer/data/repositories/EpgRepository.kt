package com.example.iptvplayer.data.repositories

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.Utils
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
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
    fun getEpgYearRef(
        countryCode: String,
        channelId: String
    ): DocumentReference {
        Log.i("epg country code", countryCode)
        Log.i("epg channel id", channelId)

        return firestore
            .collection("epg_prod").document("GE")
            .collection("channels_id").document(channelId)
            .collection("years").document("2025")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEpgMonthsRef(countryCode: String, channelId: String): CollectionReference {
        return getEpgYearRef(countryCode, channelId).collection("months")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEpgDaysRef(countryCode: String, channelId: String, month: String): CollectionReference {
        return getEpgMonthsRef(countryCode, channelId)
            .document(month).collection("days")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getEpgMonth(
        countryCode: String,
        channelId: String
    ): Int {
        val epgMonth = getEpgMonthsRef(countryCode, channelId).get().await()
        return if (epgMonth.documents.size == 0) -1 else epgMonth.documents[0].id.toInt()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getEpgFirstAndLastDays(
        countryCode: String,
        channelId: String,
        month: String
    ): Pair<Int,Int> {
        val daysRef = getEpgDaysRef(countryCode, channelId, month).get().await()
        Log.i("epg ref exists", daysRef.documents.size.toString())

        val sortedEpgDays = daysRef.documents.sortedBy { doc -> doc.id.toInt() }
        if (sortedEpgDays.isEmpty()) return Pair(-1, -1)
        val minimumDay = sortedEpgDays[0].id.toInt()
        val maximumDay = sortedEpgDays[sortedEpgDays.size-1].id.toInt()

        val firstAndLastDay = Pair(minimumDay, maximumDay)
        Log.i("FIRST AND LAST DAY", firstAndLastDay.toString())
        return firstAndLastDay
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getFirstAndLastEpgTimestamps(
        countryCode: String,
        channelId: String,
        month: String
    ): Pair<Long, Long> {
        val epgFirstAndLastDays = getEpgFirstAndLastDays(countryCode, channelId, month)
        if (epgFirstAndLastDays.first == -1) return Pair(-1,-1)


        val firstDayEpgRef = getEpgDaysRef(countryCode, channelId, month)
            .document(epgFirstAndLastDays.first.toString())
            .collection("programmes_list")

        val lastDayEpgRef = getEpgDaysRef(countryCode, channelId, month)
            .document(epgFirstAndLastDays.second.toString())
            .collection("programmes_list")

        val firstEpgTimestamp = getFirstEpgTimestampOfDay(firstDayEpgRef)
        val lastEpgTimestamp = getLastEpgTimestampOfDay(lastDayEpgRef)

        return Pair(firstEpgTimestamp, lastEpgTimestamp)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getFirstEpgTimestampOfDay(dayRef: CollectionReference): Long {
        val firstEpgTimestamp = dayRef.orderBy("start_time").get().await()
        return firstEpgTimestamp.documents[0].id.toLong()
    }

    suspend fun getLastEpgTimestampOfDay(dayRef: CollectionReference): Long {
        val lastEpgTimestamp = dayRef.orderBy("start_time").get().await()
        return lastEpgTimestamp.documents[lastEpgTimestamp.documents.size-1].id.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getDayEpg(
        countryCode: String,
        channelId: String,
        month: String,
        day: String
    ): MutableList<Epg> {
        Log.i("Thread", "Before Firestore: ${Thread.currentThread().name}")

        val epgList = mutableListOf<Epg>()

        val currentDayGmtProgrammesList =
            getEpgDaysRef(countryCode, channelId, month)
                .document(day)
                .collection("programmes_list").get().await()

        for (epgDoc in currentDayGmtProgrammesList.documents) {
            val startTime = epgDoc["start_time"].toString().toLong()
            val stopTime = epgDoc["stop_time"].toString().toLong()
            val duration = stopTime - startTime
            val title = epgDoc["title"].toString()

            val epg = Epg(startTime, stopTime, duration, title)
            epgList += epg
            Log.i("WHAT", "${epg.toString()} $day")
        }

        Log.i("Thread", "after Firestore: ${Thread.currentThread().name}")
        Log.i("WHAT", "executed")

        return epgList.sortedBy { epg -> epg.startTime }.toMutableList()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEpgById(countryCode: String, channelId: String) = flow {
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

        val firstAndLastEpgDay = getEpgFirstAndLastDays(countryCode, channelId, currentGmtMonth.toString())
        val firstEpgDay = firstAndLastEpgDay.first
        val lastEpgDay = firstAndLastEpgDay.second

        if (firstEpgDay == -1) return@flow

        var dayCount = 0
        while (dayCount < 10) {
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
                countryCode,
                channelId,
                currentGmtMonth.toString(),
                localGmtDay.toString()
            )


            startTimeSortedEpg.add(0, dummyEpg)
            Log.i("WHY", startTimeSortedEpg.toString())
            Log.i("WHY", localGmtDay.toString())
            if (startTimeSortedEpg.size > 1) emit(startTimeSortedEpg)
            dayCount++
        }
    }
}