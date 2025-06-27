package com.example.iptvplayer.unitTests.media

import app.cash.turbine.turbineScope
import com.example.iptvplayer.data.IjkPlayerFactory
import com.example.iptvplayer.domain.media.MediaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import tv.danmaku.ijk.media.player.IjkMediaPlayer


@ExtendWith(MockitoExtension::class)
class MediaManagerTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var ijkPlayerFactory: IjkPlayerFactory
    @Mock private lateinit var ijkMediaPlayer: IjkMediaPlayer

    private lateinit var mediaManager: MediaManager

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun initializePlayer_appStartup_allVariablesRelatedToPlayerInitAreSetCorrectly() = runTest {
        turbineScope {
            whenever(ijkPlayerFactory.create()).thenReturn(ijkMediaPlayer)

            mediaManager = MediaManager(
                ijkPlayerFactory = ijkPlayerFactory,
                managerScope = backgroundScope
            )

            val ijkPlayerCollector = mediaManager.ijkPlayer.testIn(this)
            assertNull(ijkPlayerCollector.awaitItem())
            advanceTimeBy(1)
            assertEquals(ijkMediaPlayer, ijkPlayerCollector.awaitItem())
            ijkPlayerCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}