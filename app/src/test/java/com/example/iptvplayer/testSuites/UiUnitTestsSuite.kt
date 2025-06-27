package com.example.iptvplayer.testSuites

import com.example.iptvplayer.unitTests.init.InitViewModelTest
import com.example.iptvplayer.unitTests.playback.PlaybackControlsViewModelTest
import com.example.iptvplayer.unitTests.time.DateAndTimeViewModelTest
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite

@Suite
@SelectClasses(
    InitViewModelTest::class,
    PlaybackControlsViewModelTest::class,
    DateAndTimeViewModelTest::class
)
class UiUnitTestsSuite