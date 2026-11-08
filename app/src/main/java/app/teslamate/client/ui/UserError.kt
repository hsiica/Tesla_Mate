package app.teslamate.client.ui

import android.content.Context
import app.teslamate.client.R
import app.teslamate.client.data.api.AppException

sealed interface UserError {
    data object NotConfigured : UserError
    data class Api(val message: String) : UserError
    data class Http(val code: Int, val serverMessage: String?) : UserError
    data object Network : UserError
    data object Parse : UserError
}

fun Throwable.toUserError(): UserError = when (this) {
    is AppException.NotConfigured -> UserError.NotConfigured
    is AppException.Api -> UserError.Api(message.orEmpty())
    is AppException.Http -> UserError.Http(code, serverMessage)
    is AppException.Network -> UserError.Network
    is AppException.Parse -> UserError.Parse
    else -> UserError.Network
}

fun UserError.message(context: Context): String = when (this) {
    UserError.NotConfigured -> context.getString(R.string.error_not_configured)
    is UserError.Api -> context.getString(R.string.error_api, message)
    is UserError.Http -> if (serverMessage.isNullOrBlank()) {
        context.getString(R.string.error_http, code)
    } else {
        context.getString(R.string.error_http_message, code, serverMessage)
    }
    UserError.Network -> context.getString(R.string.error_network)
    UserError.Parse -> context.getString(R.string.error_parse)
}
