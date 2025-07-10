package com.example.iptvplayer.domain.archive

import android.util.Log
import com.example.iptvplayer.retrofit.data.DvrRange
import com.example.iptvplayer.view.archive.CurrentDvrInfoState
import com.example.iptvplayer.view.archive.DvrDaysRange
import com.example.iptvplayer.view.archive.DvrMonthsRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveManager @Inject constructor(
    private val getDvrRangesUseCase: GetDvrRangesUseCase
) {

    private val _rewindError: MutableStateFlow<String> = MutableStateFlow("")
    val rewindError: StateFlow<String> = _rewindError

    private val _currentChannelDvrInfoState: MutableStateFlow<CurrentDvrInfoState> =
        MutableStateFlow(CurrentDvrInfoState.LOADING)
    val currentChannelDvrInfoState: StateFlow<CurrentDvrInfoState> = _currentChannelDvrInfoState

    private val _focusedChannelDvrInfoState: MutableStateFlow<CurrentDvrInfoState> = MutableStateFlow(
        CurrentDvrInfoState.LOADING
    )
    val focusedChannelDvrInfoState: StateFlow<CurrentDvrInfoState> = _focusedChannelDvrInfoState

    private val _archiveSegmentUrl: MutableStateFlow<String> = MutableStateFlow("")
    val archiveSegmentUrl: StateFlow<String> = _archiveSegmentUrl

    private val _currentChannelDvrRanges: MutableStateFlow<List<DvrRange>> = MutableStateFlow(
        emptyList()
    )
    val currentChannelDvrRanges: StateFlow<List<DvrRange>> = _currentChannelDvrRanges

    private val _currentChannelDvrRange: MutableStateFlow<Int> = MutableStateFlow(-1)
    val currentChannelDvrRange: StateFlow<Int> = _currentChannelDvrRange

    private val _focusedChannelDvrRanges: MutableStateFlow<List<DvrRange>> = MutableStateFlow(
        emptyList()
    )
    val focusedChannelDvrRanges: StateFlow<List<DvrRange>> = _focusedChannelDvrRanges

    private val _focusedChannelDvrRange: MutableStateFlow<Int> = MutableStateFlow(-1)
    val focusedChannelDvrRange: StateFlow<Int> = _focusedChannelDvrRange

    // for example 3 march -> 3
    private val _dvrFirstAndLastDay: MutableStateFlow<DvrDaysRange> = MutableStateFlow(DvrDaysRange())
    val dvrFirstAndLastDay: StateFlow<DvrDaysRange> = _dvrFirstAndLastDay

    private val _dvrFirstAndLastMonth: MutableStateFlow<DvrMonthsRange> = MutableStateFlow(
        DvrMonthsRange()
    )
    val dvrFirstAndLastMonth: StateFlow<DvrMonthsRange> = _dvrFirstAndLastMonth

    var focusedChannelDvrCollectionJob: Job? = null
    var currentChannelDvrCollectionJob: Job? = null

    fun updateDvrFirstAndLastDay(dvrDaysRange: DvrDaysRange) {
        _dvrFirstAndLastDay.value = dvrDaysRange
    }

    fun updateDvrFirstAndLastMonth(dvrMonthsRange: DvrMonthsRange) {
        _dvrFirstAndLastMonth.value = dvrMonthsRange
    }

    fun updateChannelCurrentDvrRange(isCurrent: Boolean, rangeIndex: Int) {
        println("update range $rangeIndex")
        if (isCurrent) _currentChannelDvrRange.value = rangeIndex
        else _focusedChannelDvrRange.value = rangeIndex
    }

    fun updateCurrentChannelDvrRanges(isCurrent: Boolean, ranges: List<DvrRange>) {
        if (isCurrent) _currentChannelDvrRanges.value = ranges
        else _focusedChannelDvrRanges.value = ranges
    }

    fun updateDvrInfoState(isCurrent: Boolean, state: CurrentDvrInfoState) {
        if (isCurrent) _currentChannelDvrInfoState.value = state
        else _focusedChannelDvrInfoState.value = state
    }

    fun setRewindError(error: String) {
        _rewindError.value = error
    }

    suspend fun isStreamWithinDvrRange(newTime: Long): Boolean =
        withContext(Dispatchers.IO) {
            val dvrRanges = currentChannelDvrRanges
                .filter { it.isNotEmpty() }
                .first()
            Log.i("dvr range collected", dvrRanges.toString())
            //Log.i("dvr range compare", "${Utils.formatDate(dvrRange.first, datePattern)}")
            //Log.i("dvr range compare", "${Utils.formatDate(newTime, datePattern)}")
            //Log.i("dvr range compare", "${Utils.formatDate(dvrRange.second, datePattern)}")

            var isWithinDvrRange = false

            for (dvrRange in dvrRanges) {
                val isMoreThanFirstBound = newTime >= dvrRange.from
                val isLessThanSecondBound = newTime <= dvrRange.from + dvrRange.duration
                if (isMoreThanFirstBound && isLessThanSecondBound) {
                    isWithinDvrRange = true
                    break
                }
            }

            isWithinDvrRange
        }

    suspend fun getArchiveUrl(url: String, currentTime: Long) {
        if (url == "") return
        Log.i("get archive url method", "called")
        val datePattern = "EEEE d MMMM HH:mm:ss"
        //Log.i("REALLY", Utils.formatDate(currentTime, datePattern))
        Log.i("base url given", url.toString())
        val baseUrl = url.substring(0, url.lastIndexOf("/") + 1)
        val token = url.substring(url.lastIndexOf("=") + 1, url.length)

        val currentDvrRange = currentChannelDvrRanges.value[currentChannelDvrRange.value]
        val currentDvrRangeStopTime = currentDvrRange.from + currentDvrRange.duration

        var archiveBufferedSecondsLength = 180L

        if (currentTime + 180 > currentDvrRangeStopTime) {
            archiveBufferedSecondsLength = 180 - ((currentTime + 180) - 180)
        }

        val archiveUrl = baseUrl + "index-$currentTime-$archiveBufferedSecondsLength.m3u8?token=$token"
        Log.i("base url", "$baseUrl $token $archiveUrl")
        // checking again, because if the rewind was not continuous, time did not change,
        // therefore still in present, rewinding to current time would result in
        // file not found exception
        if (isStreamWithinDvrRange(currentTime)) {
            _archiveSegmentUrl.value = archiveUrl
        }
    }

    fun setDvrRanges(isCurrentChannel: Boolean, dvrRanges: List<DvrRange>) {
        updateCurrentChannelDvrRanges(isCurrentChannel, dvrRanges)
        if (dvrRanges.isEmpty()) {
            updateDvrInfoState(isCurrentChannel, CurrentDvrInfoState.NOT_AVAILABLE_GLOBAL)
        } else {
            println("available global?")
            updateDvrInfoState(isCurrentChannel, CurrentDvrInfoState.AVAILABLE_GLOBAL)
        }
    }
}