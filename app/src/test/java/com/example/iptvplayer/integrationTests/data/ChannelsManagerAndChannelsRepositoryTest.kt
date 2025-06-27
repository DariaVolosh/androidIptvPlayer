package com.example.iptvplayer.integrationTests.data

import android.content.Context
import com.example.iptvplayer.data.repositories.ChannelsRepository
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.retrofit.data.BackendInfoResponse
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.ChannelsBackendInfoResponse
import com.example.iptvplayer.retrofit.data.StreamUrlTemplate
import com.example.iptvplayer.retrofit.data.StreamUrlTemplates
import com.example.iptvplayer.retrofit.data.StreamUrlTemplatesResponse
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ChannelsManagerAndChannelsRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var context: Context
    @Mock private lateinit var channelsAndEpgService: ChannelsAndEpgService

    private lateinit var channelsManager: ChannelsManager
    private lateinit var channelsRepository: ChannelsRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        channelsRepository = ChannelsRepository(
            context = context,
            channelsAndEpgService = channelsAndEpgService
        )

        channelsManager = ChannelsManager(
            channelsRepository = channelsRepository
        )
    }

    @Test
    fun getChannelsData_channelsDataAndStreamsUrlTemplatesAvailable_channelsDataReturned() = runTest {
        val mockChannelsErrorCallback: Function2<String, String, Unit> = mock()
        val mockChannelsData = List(5) {index -> BackendInfoResponse(
            channel = listOf(
                ChannelData(
                    name = "Channel ${index + 1}"
                )
            )
        ) }
        val mockTemplatesResponse = StreamUrlTemplatesResponse(
            data = StreamUrlTemplates(
                templates = List(2) {index -> StreamUrlTemplate("Template ${index + 1}") }
            )
        )
        val mockChannelsResponse = ChannelsBackendInfoResponse(
            data = mockChannelsData
        )

        whenever(channelsAndEpgService.getStreamsUrlTemplates(any())).thenReturn(mockTemplatesResponse)
        whenever(channelsAndEpgService.getChannelsInfo()).thenReturn(mockChannelsResponse)

        val response = channelsManager.getChannelsData(mockChannelsErrorCallback)
        assertEquals(mockChannelsData.map { res -> res.channel[0] }, response)

        verify(channelsAndEpgService, times(1)).getStreamsUrlTemplates(any())
        verify(channelsAndEpgService, times(1)).getChannelsInfo()
    }

    @Test
    fun getChannelsData_channelsDataAndStreamsUrlTemplatesNotAvailable_emptyChannelsDataReturned() = runTest {
        val mockChannelsErrorCallback: Function2<String, String, Unit> = mock()
        val mockTemplatesResponse = StreamUrlTemplatesResponse(
            data = null
        )

        whenever(context.getString(any())).thenAnswer { "" }
        whenever(channelsAndEpgService.getStreamsUrlTemplates(any())).thenReturn(mockTemplatesResponse)

        val response = channelsManager.getChannelsData(mockChannelsErrorCallback)

        assertEquals(emptyList<ChannelData>(), response)

        verify(channelsAndEpgService, times(1)).getStreamsUrlTemplates(any())
        verify(channelsAndEpgService, never()).getChannelsInfo()
    }

    @Test
    fun getChannelsData_channelsDataNotAvailableButStreamsUrlTemplatesAvailable_emptyChannelsDataReturned() = runTest {
        val mockChannelsErrorCallback: Function2<String, String, Unit> = mock()
        val mockTemplatesResponse = StreamUrlTemplatesResponse(
            data = StreamUrlTemplates(
                templates = List(2) {index -> StreamUrlTemplate("Template ${index + 1}") }
            )
        )
        val mockChannelsDataResponse = ChannelsBackendInfoResponse(
            data = null
        )

        whenever(context.getString(any())).thenAnswer { "" }
        whenever(channelsAndEpgService.getStreamsUrlTemplates(any())).thenReturn(mockTemplatesResponse)
        whenever(channelsAndEpgService.getChannelsInfo()).thenReturn(mockChannelsDataResponse)

        val response = channelsManager.getChannelsData(mockChannelsErrorCallback)

        assertEquals(emptyList<ChannelData>(), response)

        verify(channelsAndEpgService, times(1)).getStreamsUrlTemplates(any())
        verify(channelsAndEpgService, times(1)).getChannelsInfo()
    }
}