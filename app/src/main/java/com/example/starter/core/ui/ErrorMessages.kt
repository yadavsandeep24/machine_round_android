package com.example.starter.core.ui

import com.example.starter.R
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

/** Presentation concern: turns a failure into text for the user. */
fun Throwable.toUserMessage(): UiText = when (this) {
    is UnknownHostException -> UiText.Res(R.string.error_no_internet)
    is SocketTimeoutException -> UiText.Res(R.string.error_timeout)
    is HttpException -> UiText.Res(R.string.error_server, listOf(code()))
    else -> message?.let(UiText::Dynamic) ?: UiText.Res(R.string.error_generic)
}
