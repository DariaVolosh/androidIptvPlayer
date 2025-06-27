package com.example.iptvplayer.unitTests.media

import com.example.iptvplayer.data.InputStreamProvider
import com.example.iptvplayer.data.repositories.MediaDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream

class MediaDataSourceTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var inputStreamProvider: InputStreamProvider

    private lateinit var mediaDataSource: MediaDataSource

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        mediaDataSource = MediaDataSource(inputStreamProvider)
    }

    @Test
    fun setMediaUrl_urlAvailable_initializesInputStream() = runTest {
        val mockUrl = "url"
        mediaDataSource.setMediaUrl(mockUrl)

        verify(inputStreamProvider, times(1)).getStream(mockUrl)
    }

    @Test
    fun readAt_inputStreamAvailable_returnsBytesRead() = runTest {
        val mockUrl = "url"
        val bufferSize = 30
        val testData = "Test data of the input stream in bytes".toByteArray()
        val mockInputStream = ByteArrayInputStream(testData)

        whenever(inputStreamProvider.getStream(mockUrl)).thenReturn(mockInputStream)
        mediaDataSource.setMediaUrl(mockUrl)

        val buffer = ByteArray(bufferSize)
        val bytesRead = mediaDataSource.readAt(0, buffer, 0, bufferSize)
        assertEquals(bufferSize, bytesRead)
    }

    @Test
    fun readAt_EOFreached_requestsNextSegmentOnlyOnce() = runTest {
        val mockUrl = "url"
        val bufferSize = 30
        val testData = "Test data of the input stream in bytes".toByteArray()
        val testDataSize = testData.size
        val mockInputStream = ByteArrayInputStream(testData)

        whenever(inputStreamProvider.getStream(mockUrl)).thenReturn(mockInputStream)
        mediaDataSource.setMediaUrl(mockUrl)
        val mockOnNextSegmentCallback = mock<() -> Unit>()
        mediaDataSource.setOnNextSegmentRequestedCallback(mockOnNextSegmentCallback)

        val buffer = ByteArray(bufferSize)
        var bytesRead = mediaDataSource.readAt(0, buffer, 0, bufferSize)
        assertEquals(bufferSize, bytesRead)
        val bytesLeftToRead = testDataSize - bytesRead
        bytesRead = mediaDataSource.readAt(0, buffer, 0, bufferSize) // read al the bytes
        assertEquals(bytesLeftToRead, bytesRead)
        bytesRead = mediaDataSource.readAt(0, buffer, 0, bufferSize)
        assertEquals(0, bytesRead)
        verify(mockOnNextSegmentCallback, times(1)).invoke()

        bytesRead = mediaDataSource.readAt(0, buffer, 0, bufferSize)
        assertEquals(0, bytesRead)
        verify(mockOnNextSegmentCallback, times(1)).invoke()
    }
}