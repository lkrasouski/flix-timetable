package com.flix.timetable.data

import com.flix.timetable.core.DataError
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

internal fun Throwable.toDataError(): DataError =
    when (this) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> DataError.Timeout
        is ServerResponseException -> DataError.Server(response.status.value)
        is ClientRequestException -> DataError.Client(response.status.value)
        is ResponseException -> DataError.Unknown
        is ContentConvertException, is SerializationException, is NoTransformationFoundException ->
            DataError.InvalidResponse
        is IOException -> DataError.NoConnection
        else -> DataError.Unknown
    }
