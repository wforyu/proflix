package com.proflix.network.di

import android.content.Context
import com.proflix.common.utils.Constants
import com.proflix.network.interceptor.CacheInterceptor
import com.proflix.network.interceptor.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.CookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        prettyPrint = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cacheInterceptor: CacheInterceptor,
        retryInterceptor: RetryInterceptor,
        cache: Cache
    ): OkHttpClient {
        val cookieStore = mutableMapOf<String, List<Cookie>>()

        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        }

        val builder = OkHttpClient.Builder()
            .cache(cache)
            .cookieJar(cookieJar)
            .addInterceptor(cacheInterceptor)
            .addInterceptor(retryInterceptor)
            .connectTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .connectionPool(
                okhttp3.ConnectionPool(
                    maxIdleConnections = 10,
                    keepAliveDuration = 5,
                    timeUnit = TimeUnit.MINUTES
                )
            )

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        builder.addInterceptor(loggingInterceptor)

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideCache(@ApplicationContext context: Context): Cache {
        return Cache(
            directory = File(context.cacheDir, "http_cache"),
            maxSize = Constants.CACHE_SIZE
        )
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}
