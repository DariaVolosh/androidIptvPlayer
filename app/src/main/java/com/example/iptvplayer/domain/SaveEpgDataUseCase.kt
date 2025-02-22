package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.EpgRepository
import com.example.iptvplayer.data.repositories.FileUtilsRepository
import javax.inject.Inject

class SaveEpgDataUseCase @Inject constructor(
    private val epgRepository: EpgRepository,
    private val fileUtilsRepository: FileUtilsRepository
) {
    suspend fun saveEpgData(channelNames: Set<String>) {
        val epgInputStream = fileUtilsRepository.getFileInputStream(
            "http://kfjb.shott.top/e/1c2feecdbd1b9a91b24ce0a2a5cf85a3/light.xmltv.gz"
        )

        val gzipInputStream = fileUtilsRepository.unzipGzip(epgInputStream)

        val data = epgRepository.parseEpgChannelsData(gzipInputStream, channelNames)
    }
}