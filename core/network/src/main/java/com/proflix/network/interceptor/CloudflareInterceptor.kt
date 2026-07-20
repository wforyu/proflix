package com.proflix.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class CloudflareInterceptor @Inject constructor() : Interceptor {

    companion object {
        private const val TAG = "CloudflareInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val enhancedRequest = originalRequest.newBuilder()
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .addHeader("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Cache-Control", "max-age=0")
            .addHeader("Sec-Ch-Ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"")
            .addHeader("Sec-Ch-Ua-Mobile", "?0")
            .addHeader("Sec-Ch-Ua-Platform", "\"Windows\"")
            .addHeader("Sec-Fetch-Dest", "document")
            .addHeader("Sec-Fetch-Mode", "navigate")
            .addHeader("Sec-Fetch-Site", "none")
            .addHeader("Sec-Fetch-User", "?1")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader("Connection", "keep-alive")
            .build()

        val response = chain.proceed(enhancedRequest)

        if (isCloudflareChallenge(response)) {
            Log.w(TAG, "Cloudflare challenge detected for: ${originalRequest.url}")
            response.close()
            return handleCloudflareChallenge(chain, originalRequest)
        }

        return response
    }

    private fun isCloudflareChallenge(response: Response): Boolean {
        if (response.code == 403 || response.code == 503) {
            val body = response.peekBody(1024 * 10).string()
            return body.contains("challenge-platform") ||
                    body.contains("Just a moment") ||
                    body.contains("Checking if the site connection is secure") ||
                    body.contains("cf-challenge") ||
                    body.contains("Enable JavaScript and cookies to continue")
        }
        return false
    }

    private fun handleCloudflareChallenge(chain: Interceptor.Chain, originalRequest: okhttp3.Request): Response {
        Log.d(TAG, "Attempting to bypass Cloudflare challenge for: ${originalRequest.url}")

        val retryRequest = originalRequest.newBuilder()
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .build()

        return chain.proceed(retryRequest)
    }
}
