package com.example.iptvplayer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

@Module
@InstallIn(SingletonComponent::class)
class XmlPullParserModule {
    @Provides
    fun provideXmlPullParser(): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        val xmlPullParser = factory.newPullParser()
        return xmlPullParser
    }
}