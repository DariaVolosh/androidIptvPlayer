package com.example.iptvplayer.testSuites

import com.example.iptvplayer.integrationTests.domain.ArchiveOrchestratorAndArchiveManagerTest
import com.example.iptvplayer.unitTests.channels.ChannelsManagerTest
import com.example.iptvplayer.unitTests.channels.ChannelsOrchestratorTest
import com.example.iptvplayer.unitTests.media.MediaManagerTest
import com.example.iptvplayer.unitTests.time.TimeManagerTest
import com.example.iptvplayer.unitTests.time.TimeOrchestratorTest
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite

@Suite
@SelectClasses(
    // unit tests - orchestrators
    ChannelsOrchestratorTest::class,
    TimeOrchestratorTest::class,
    // unit tests - managers
    ArchiveOrchestratorAndArchiveManagerTest::class,
    ChannelsManagerTest::class,
    MediaManagerTest::class,
    TimeManagerTest::class
)
class DomainUnitTestsSuite
