package com.flix.timetable.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.flix.timetable.ApiConfig
import com.flix.timetable.data.DefaultTimetableRepository
import com.flix.timetable.data.DeparturesMapper
import com.flix.timetable.data.remote.TimetableApi
import com.flix.timetable.data.remote.createHttpClient
import com.flix.timetable.domain.GetUpcomingDeparturesUseCase
import com.flix.timetable.domain.TimetableRepository
import com.flix.timetable.presentation.TimetableViewModel
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.time.Clock

/** Platform HTTP engine: OkHttp on Android/JVM, NSURLSession on iOS. */
internal expect fun createPlatformEngine(): HttpClientEngine

/** Everything the timetable feature needs. The host app only provides [config]. */
public fun sharedModule(config: ApiConfig): Module =
    module {
        single { config }
        single<Clock> { Clock.System }
        single {
            Logger.setMinSeverity(if (config.isDebug) Severity.Debug else Severity.Warn)
            Logger.withTag("FlixTimetable")
        }
        single { createHttpClient(createPlatformEngine(), config, get()) }
        single { TimetableApi(get(), config) }
        single { DeparturesMapper(get()) }
        single<TimetableRepository> { DefaultTimetableRepository(get(), get(), get()) }
        factory { GetUpcomingDeparturesUseCase(get(), get()) }
        viewModel { TimetableViewModel(get(), get()) }
    }
