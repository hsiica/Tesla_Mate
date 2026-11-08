package app.teslamate.client.data.api

import app.teslamate.client.data.settings.ApiConfigState
import app.teslamate.client.data.settings.ApiEndpoint
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BaseUrlInterceptorTest {
    @Test
    fun rewritesOriginAndAddsBearerToken() {
        val state = ApiConfigState()
        state.endpoint = ApiEndpoint("http://192.168.1.10:8080/garage", "secret-token")
        val captured = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(state))
            .addInterceptor(CaptureInterceptor(captured))
            .build()
        client.newCall(
            Request.Builder().url("http://127.0.0.1/api/v1/cars/1/status?page=1").build(),
        ).execute().close()
        val request = captured.get()
        assertEquals("http://192.168.1.10:8080/garage/api/v1/cars/1/status?page=1", request.url.toString())
        assertEquals("Bearer secret-token", request.header("Authorization"))
    }

    @Test
    fun omitsAuthorizationWhenTokenBlank() {
        val state = ApiConfigState()
        state.endpoint = ApiEndpoint("https://tm.example.com", "")
        val captured = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(state))
            .addInterceptor(CaptureInterceptor(captured))
            .build()
        client.newCall(Request.Builder().url("http://127.0.0.1/api/v1/cars").build()).execute().close()
        assertEquals("https://tm.example.com/api/v1/cars", captured.get().url.toString())
        assertNull(captured.get().header("Authorization"))
    }

    @Test
    fun failsWhenBaseUrlMissing() {
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(ApiConfigState()))
            .build()
        assertThrows(Exception::class.java) {
            client.newCall(Request.Builder().url("http://127.0.0.1/api/v1/cars").build()).execute()
        }
    }

    private class CaptureInterceptor(private val slot: AtomicReference<Request>) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            slot.set(chain.request())
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody())
                .build()
        }
    }
}
