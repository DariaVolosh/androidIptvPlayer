package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.data.Epg
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await
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

    suspend fun getEpgById(id: String, countryCode: String): List<Epg> {
        val monthsResult = firestore
            .collection("epg_prod").document(countryCode)
            .collection("channels_id").document(id)
            .collection("years").document("2025")
            .collection("months").get().await()

        val epgList = mutableListOf<Epg>()

        for (month in monthsResult.documents) {
            val daysResult = month.reference.collection("days").get().await()

            for (day in daysResult.documents) {
                val epgResult = day.reference.collection("programmes_list").get().await()
                val epgDocumentResult = epgResult.documents[0].reference.get().await()

                epgDocumentResult.data?.let { epgData ->
                    for (key in epgData.toSortedMap().keys) {
                        try {
                            val epgInfo = epgData[key] as Map<*,*>
                            val epg = Epg(
                                key.toLong(),
                                epgInfo["stop_time"].toString().toLong(),
                                epgInfo["stop_time"].toString().toLong() - key.toLong(),
                                epgInfo["title"].toString()
                            )
                            epgList += epg
                        } catch (e: Exception) {

                        }
                    }
                }
            }
        }

        return epgList
    }
}