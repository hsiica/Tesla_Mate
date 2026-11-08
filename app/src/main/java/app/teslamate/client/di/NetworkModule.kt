package app.teslamate.client.di

import app.teslamate.client.BuildConfig
import app.teslamate.client.data.api.ApiJson
import app.teslamate.client.data.api.BaseUrlInterceptor
import app.teslamate.client.data.api.TeslaMateApi
import app.teslamate.client.data.settings.ApiConfigState
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttp(apiConfigState: ApiConfigState): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(BaseUrlInterceptor(apiConfigState))
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Accept", "application/json")
                        .header("User-Agent", "TeslaMateAndroid/${BuildConfig.VERSION_NAME}")
                        .build(),
                )
            }
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization")
                },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideApi(client: OkHttpClient): TeslaMateApi {
        return Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(ApiJson.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TeslaMateApi::class.java)
    }

    private const val PLACEHOLDER_BASE_URL = "http://127.0.0.1/"
}
