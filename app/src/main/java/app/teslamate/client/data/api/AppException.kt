package app.teslamate.client.data.api

sealed class AppException(message: String) : Exception(message) {
    class NotConfigured : AppException("not configured")
    class Api(message: String) : AppException(message)
    class Http(val code: Int, val serverMessage: String?) : AppException(serverMessage ?: "HTTP $code")
    class Network(cause: Throwable) : AppException(cause.message ?: "network")
    class Parse(cause: Throwable) : AppException(cause.message ?: "parse")
}
