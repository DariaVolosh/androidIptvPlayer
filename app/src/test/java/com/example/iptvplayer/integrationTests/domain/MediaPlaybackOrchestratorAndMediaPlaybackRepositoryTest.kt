package com.example.iptvplayer.integrationTests.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class MediaPlaybackOrchestratorAndMediaPlaybackRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup(){
        Dispatchers.setMain(testDispatcher)
    }


}