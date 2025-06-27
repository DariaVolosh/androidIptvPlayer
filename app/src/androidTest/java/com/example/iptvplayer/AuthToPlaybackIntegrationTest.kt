package com.example.iptvplayer

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.iptvplayer.domain.auth.GetAuthTokenUseCase
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.media.GetMediaDataSourceUseCase
import com.example.iptvplayer.domain.media.HandleNextSegmentRequestedUseCase
import com.example.iptvplayer.domain.media.SetMediaUrlUseCase
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.view.auth.AuthViewModel
import com.example.iptvplayer.view.channelsAndEpgRow.ChannelsAndEpgRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations


class AuthToPlaybackIntegrationTest {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockWebServer: MockWebServer

    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    @Mock private lateinit var getMediaDataSourceUseCase: GetMediaDataSourceUseCase
    @Mock private lateinit var handleNextSegmentRequestedUseCase: HandleNextSegmentRequestedUseCase
    @Mock private lateinit var setMediaUrlUseCase: SetMediaUrlUseCase

    @Mock private lateinit var getAuthTokenUseCase: GetAuthTokenUseCase
    @Mock private lateinit var errorManager: ErrorManager

    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start(8080)
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        authViewModel = AuthViewModel(
            getAuthTokenUseCase = getAuthTokenUseCase,
            errorManager = errorManager
        )
    }

    @Test
    fun playbackFlow_fetchChannels_playsCurrentChannel() =
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody("""
                    {"data": {"token": "dummy token"}}
                """)
            )

            composeTestRule.setContent {
                ChannelsAndEpgRow()
            }

            composeTestRule.waitForIdle()
            // check if token fetch method was called
            verify(authViewModel).getBackendToken()
        }
}