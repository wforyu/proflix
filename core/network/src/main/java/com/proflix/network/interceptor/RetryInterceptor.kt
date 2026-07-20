package com.proflix.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RetryInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var retryCount = 0

        while (response.code >= 500 && retryCount < 2) {
            retryCount++
            try {
                response.close()
            } catch (_: Exception) {}
            response = chain.proceed(request)
        }

        return response
    }
}
