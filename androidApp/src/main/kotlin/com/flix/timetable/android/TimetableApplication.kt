package com.flix.timetable.android

import android.app.Application
import com.flix.timetable.ApiConfig
import com.flix.timetable.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TimetableApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TimetableApplication)
            modules(
                sharedModule(
                    ApiConfig(
                        baseUrl = BuildConfig.API_BASE_URL,
                        apiKey = BuildConfig.API_KEY,
                        stationId = BuildConfig.STATION_ID,
                        isDebug = BuildConfig.DEBUG,
                    ),
                ),
            )
        }
    }
}
