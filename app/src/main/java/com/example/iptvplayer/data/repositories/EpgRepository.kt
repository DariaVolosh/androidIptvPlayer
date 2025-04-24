package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.retrofit.data.EpgDataAndCurrentIndex
import com.example.iptvplayer.retrofit.data.EpgResponse
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CompletableDeferred
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.TimeZone
import javax.inject.Inject

class EpgRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val channelsAndEpgService: ChannelsAndEpgService
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

    /*fun getEpgYearRef(
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

    suspend fun getFirstAndLastMonths(
        channelId: String
    ): Pair<Int, Int> {
        Log.i("get epg month", channelId)
        val epgMonthsRef = getEpgMonthsRef(channelId).get().await()
        if (epgMonthsRef.documents.isEmpty()) return Pair(-1, -1)
        val firstMonth = epgMonthsRef.documents[0].id.toInt()
        val lastMonth = epgMonthsRef.documents[epgMonthsRef.documents.size-1].id.toInt()
        return Pair(firstMonth, lastMonth)
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
        channelId: String
    ): Pair<Long, Long> {
        val firstAndLastEpgMonths = getFirstAndLastMonths(channelId)
        val firstMonth = firstAndLastEpgMonths.first
        val lastMonth = firstAndLastEpgMonths.second

        val epgFirstMonthDaysRange = getEpgFirstAndLastDays(channelId, firstMonth.toString())
        if (epgFirstMonthDaysRange.first == -1) return Pair(-1,-1)
        val epgLastMonthDaysRange = getEpgFirstAndLastDays(channelId, lastMonth.toString())

        val firstDayEpgRef = getEpgDaysRef(channelId, firstMonth.toString())
            .document(epgFirstMonthDaysRange.first.toString())
            .collection("programmes_list")

        val lastDayEpgRef = getEpgDaysRef(channelId, lastMonth.toString())
            .document(epgLastMonthDaysRange.second.toString())
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
        Log.i("day data", "$month $day $dvrRange")

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
        Log.i("epg id", channelId.toString())
        val currentGmtTime = Utils.getGmtTime()
        val currentGmtCalendar = Utils.getCalendar(currentGmtTime, TimeZone.getTimeZone("Z"))
        var currentGmtDay = Utils.getCalendarDay(currentGmtCalendar) // 1 april
        var currentGmtMonth = Utils.getCalendarMonth(currentGmtCalendar) + 1 // 04 (april)

        val datePattern = "EEEE d MMMM HH:mm:ss"

        val firstAndLastEpgTimestamps = getFirstAndLastEpgTimestamps(channelId)
        Log.i("first and last epg timestamps", "${Utils.formatDate(firstAndLastEpgTimestamps.first, datePattern)}")
        Log.i("first and last epg timestamps", "${Utils.formatDate(firstAndLastEpgTimestamps.second, datePattern)}")

        var lastEpgDay = Utils.getCalendarDay(Utils.getCalendar(firstAndLastEpgTimestamps.second, TimeZone.getTimeZone("Z")))
        var lastEpgMonth = Utils.getCalendarMonth(Utils.getCalendar(firstAndLastEpgTimestamps.second, TimeZone.getTimeZone("Z"))) + 1

        if (currentGmtDay > lastEpgDay) {
            currentGmtDay = lastEpgDay
            currentGmtMonth = lastEpgMonth
        }

        var previousGmtDay = currentGmtDay - 1 // 0 -> should switch to previous month
        var nextGmtDay = currentGmtDay + 1 // 2 april
        var previousGmtMonth = currentGmtMonth // initially current month (april)
        var nextGmtMonth = currentGmtMonth // initially current month (april)

        var isPreviousDayEpgFetched = false

        var allPreviousDaysFetched = false
        var allNextDaysFetched = false

        // dummy epg to indicate that all the epgs of the day are fetched
        // this is needed to calculate the positions of the epg programmes in overall list
        // of epg in UI
        var dummyEpg: Epg

        // fetching current day epg
        val startTimeSortedEpg = getDayEpg(
            channelId,
            currentGmtMonth.toString(),
            currentGmtDay.toString(),
            dvrRange
        )

        if (startTimeSortedEpg.isNotEmpty()) {
            // dummy epg that indicates that all the epg of the current day are being fetched
            dummyEpg = Epg(-1,-1,-1,"")
            startTimeSortedEpg.add(0, dummyEpg)
            emit(startTimeSortedEpg)
        }


        // fetching previous and next days
        while (!allNextDaysFetched || !allPreviousDaysFetched) {
            Log.i("previous and all fetched", "is previous day epg fetched: $isPreviousDayEpgFetched, all previous fetched: $allPreviousDaysFetched")
            if (!isPreviousDayEpgFetched && !allPreviousDaysFetched) {
                if (previousGmtDay == 0) {
                    // choosing previous month
                    previousGmtMonth -= 1
                    previousGmtDay = Utils.getDaysOfMonth(previousGmtMonth)
                }

                Log.i("previous gmt day and month", "$previousGmtDay $previousGmtMonth")


                val startTimeSortedEpg = getDayEpg(
                    channelId,
                    previousGmtMonth.toString(),
                    previousGmtDay.toString(),
                    dvrRange
                )

                if (startTimeSortedEpg.isNotEmpty()) {
                    // dummy epg that indicates that all the epg of the previous day are being fetched
                    dummyEpg = Epg(-2,-2,-2,"")
                    startTimeSortedEpg.add(0, dummyEpg)
                    Log.i("fetched epg", "previous $previousGmtDay $currentGmtMonth $startTimeSortedEpg")
                    previousGmtDay--

                    emit(startTimeSortedEpg)
                } else {
                    allPreviousDaysFetched = true
                }

                isPreviousDayEpgFetched = !allNextDaysFetched

            } else {
                val currentMonthLastDay = Utils.getDaysOfMonth(currentGmtMonth)
                if (nextGmtDay > currentMonthLastDay) {
                    nextGmtDay = 1

                    // choosing next month
                    nextGmtMonth += 1
                }

                val startTimeSortedEpg = getDayEpg(
                    channelId,
                    nextGmtMonth.toString(),
                    nextGmtDay.toString(),
                    dvrRange
                )

                if (startTimeSortedEpg.isNotEmpty()) {
                    // dummy epg that indicates that all the epg of the next day are being fetched
                    dummyEpg = Epg(-3,-3,-3,"")
                    startTimeSortedEpg.add(0, dummyEpg)
                    Log.i("fetched epg", "next: $nextGmtDay $currentGmtMonth $startTimeSortedEpg")
                    nextGmtDay++

                    emit(startTimeSortedEpg)
                } else {
                    allNextDaysFetched = true
                }

                isPreviousDayEpgFetched = false
            }
        }
    } */


    fun getEpgTimeRangeInSeconds(
        start: String,
        stop: String,
        datePattern: String,
        timeZone: TimeZone
    ): Pair<Long, Long> {
        val startTimeSeconds = Utils.parseDate(start, datePattern, timeZone)
        val stopTimeSeconds = Utils.parseDate(stop, datePattern, timeZone)

        return Pair(startTimeSeconds, stopTimeSeconds)
    }

    fun isProgramLive(
        start: Long,
        stop: Long,
        currentTime: Long
    ): Boolean {
        return currentTime in start..stop
    }


    // VERSION NEW (IDK IF IT WILL WORKS NORMALLY LMAO)
   suspend fun getEpgById(channelId: Int, token: String): EpgDataAndCurrentIndex {
        val epgList = CompletableDeferred<EpgDataAndCurrentIndex>()
        val currentTime = Utils.getGmtTime()
        var currentEpgIndex = -1

        Log.i("GET EPG BY ID REPOSITORY", "$channelId $token")

        channelsAndEpgService.getEpgForChannel(channelId, token)
            .enqueue(object: Callback<EpgResponse> {
                override fun onResponse(
                    call: Call<EpgResponse>,
                    response: Response<EpgResponse>
                ) {
                    val datePattern = "yyyyMMddHHmmss"
                    val timezone = TimeZone.getTimeZone("GMT+04:00")

                    if (response.code() == 200) {
                        response.body()?.data?.data?.let { data ->
                            for (i in data.indices) {
                                val epg = data[i]

                                val epgTimeRangeInSeconds =
                                    getEpgTimeRangeInSeconds(epg.start, epg.stop, datePattern, timezone)
                                epg.startSeconds = epgTimeRangeInSeconds.first
                                epg.stopSeconds = epgTimeRangeInSeconds.second
                                epg.epgVideoName = epg.epgVideoName.trim()

                                val isProgramLive = isProgramLive(
                                    epgTimeRangeInSeconds.first,
                                    epgTimeRangeInSeconds.second,
                                    currentTime
                                )

                                if (isProgramLive) currentEpgIndex = i
                            }
                            Log.i("response epg data", data.toString())
                            epgList.complete(
                                EpgDataAndCurrentIndex(
                                    data,
                                    if (currentEpgIndex == -1) data.size-1 else currentEpgIndex
                                )
                            )
                            return
                        }
                    }

                    epgList.complete(EpgDataAndCurrentIndex())
                }

                override fun onFailure(
                    call: Call<EpgResponse>,
                    throwable: Throwable
                ) {
                    Log.i("response code epg error", throwable.message.toString())
                    epgList.complete(EpgDataAndCurrentIndex())
                }
            })

        return epgList.await()
    }
}