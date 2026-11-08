package app.teslamate.client.data.api

import app.teslamate.client.data.settings.ApiConfigState
import java.io.IOException
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retrofit is built with a placeholder base URL. The real origin comes from
 * DataStore and can change without rebuilding the client.
 */
class BaseUrlInterceptor(
    private val apiConfigState: ApiConfigState,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val endpoint = apiConfigState.endpoint
        val baseRaw = endpoint.baseUrl.trim()
        if (baseRaw.isEmpty()) {
            throw IOException("TeslaMateApi base URL is not configured")
        }
        val base = baseRaw.toHttpUrl()
        val request = chain.request()
        val prefix = base.encodedPath.trimEnd('/')
        val suffix = request.url.encodedPath
        val joined = (prefix + suffix).let { if (it.isEmpty()) "/" else it }
        val rewritten = request.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .encodedPath(joined)
            .build()
        val builder = request.newBuilder().url(rewritten)
        val token = endpoint.token.trim()
        if (token.isNotEmpty()) {
            builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
