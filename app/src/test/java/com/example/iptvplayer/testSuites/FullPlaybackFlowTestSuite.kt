package com.example.iptvplayer.testSuites

import com.example.iptvplayer.integrationTests.data.AuthManagerAndAuthRepositoryTest
import com.example.iptvplayer.integrationTests.data.ChannelsManagerAndChannelsRepositoryTest
import com.example.iptvplayer.integrationTests.domain.AuthOrchestratorAndAuthManagerTest
import com.example.iptvplayer.integrationTests.domain.ChannelsOrchestratorAndChannelsManagerTest
import com.example.iptvplayer.integrationTests.ui.InitViewModelAndAuthOrchestratorTest
import com.example.iptvplayer.integrationTests.ui.InitViewModelAndChannelsOrchestratorTest
import com.example.iptvplayer.unitTests.init.InitViewModelTest
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite


@Suite
@SelectClasses(
    // unit tests - ui
    InitViewModelTest::class,
    // integration tests - ui
    InitViewModelAndAuthOrchestratorTest::class,
    InitViewModelAndChannelsOrchestratorTest::class,
    // integration tests - domain
    AuthOrchestratorAndAuthManagerTest::class,
    ChannelsOrchestratorAndChannelsManagerTest::class,
    // integration tests - data
    AuthManagerAndAuthRepositoryTest::class,
    ChannelsManagerAndChannelsRepositoryTest::class
)
class FullPlaybackFlowTestSuite