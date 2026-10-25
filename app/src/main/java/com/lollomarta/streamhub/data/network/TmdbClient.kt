package com.lollomarta.streamhub.data.network

import com.lollomarta.streamhub.domain.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Client minimale per l'API TMDb v3.
 * - Supporta sia la "API Key" (v3) sia il "Read Access Token" (v4, inizia con "eyJ").
 * - Timeout, retry controllato con backoff, cache HTTP e uso della cache se offline.
 * - Non registra mai la chiave nei log.
 */
class TmdbClient(
    private val http: OkHttpClient,
    private val apiKeyProvider: suspend () -> String?,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val maxRetries: Int = 2,
    private val backoffMillis: Long = 600,
) {
    suspend fun get(path: String, params: Map<String, String?> = emptyMap()): String = withContext(Dispatchers.IO) {
        val key = apiKeyProvider()?.trim()?.takeIf { it.isNotEmpty() } ?: throw AppError.MissingApiKey
        val url = (baseUrl.trimEnd('/') + "/" + path.trimStart('/')).toHttpUrl().newBuilder().apply {
            params.forEach { (k, v) -> if (!v.isNullOrBlank()) addQueryParameter(k, v) }
            if (!isBearerToken(key)) addQueryParameter("api_key", key)
        }.build()
        val request = Request.Builder().url(url).get()
            .header("Accept", "application/json")
            .apply { if (isBearerToken(key)) header("Authorization", "Bearer $key") }
            .build()
        executeWithRetry(request)
    }

    private suspend fun executeWithRetry(request: Request): String {
        var attempt = 0
        while (true) {
            val error: AppError = try {
                return executeOnce(request)
            } catch (e: AppError) {
                if (!e.retryable) throw e
                e
            } catch (e: SocketTimeoutException) {
                AppError.Timeout
            } catch (e: IOException) {
                AppError.Network
            }
            attempt++
            if (attempt > maxRetries) {
                if (error == AppError.Network || error == AppError.Timeout) {
                    readFromCacheOrNull(request)?.let { return it }
                }
                throw error
            }
            delay(backoffMillis * attempt)
        }
    }

    private fun executeOnce(request: Request): String =
        http.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            when {
                resp.isSuccessful -> body
                resp.code == 401 -> throw AppError.InvalidApiKey
                resp.code == 404 -> throw AppError.NotFound
                resp.code == 429 -> throw AppError.RateLimited
                else -> throw AppError.Server(resp.code)
            }
        }

    /** Ultima risorsa senza rete: risposta in cache anche se scaduta. */
    private fun readFromCacheOrNull(request: Request): String? = try {
        val cached = request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
        http.newCall(cached).execute().use { if (it.isSuccessful) it.body?.string() else null }
    } catch (e: IOException) {
        null
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.themoviedb.org/3/"

        fun isBearerToken(key: String) = key.startsWith("eyJ")

        fun buildHttpClient(cache: okhttp3.Cache?, cacheSeconds: Int = 600): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(25, TimeUnit.SECONDS)
                .cache(cache)
                .addNetworkInterceptor(cacheInterceptor(cacheSeconds))
                .build()

        /** Le risposte del catalogo cambiano lentamente: cache di 10 minuti. */
        private fun cacheInterceptor(seconds: Int) = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.isSuccessful) {
                response.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, max-age=$seconds")
                    .build()
            } else response
        }
    }
}
