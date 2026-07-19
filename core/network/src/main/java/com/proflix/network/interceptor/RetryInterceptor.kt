package com.proflix.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RetryInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var retryCount = 0

        while (!response.isSuccessful && retryCount < 3) {
            retryCount++
            response.close()
            response = chain.proceed(request)
        }

        return response
    }
}
