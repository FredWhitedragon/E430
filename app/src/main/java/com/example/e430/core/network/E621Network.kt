package com.example.e430.core.network

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Credentials
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

const val E430_USER_AGENT = "E430/0.1 (by FredWd on e621)"

enum class E621Site(val baseUrl: String, val displayName: String) {
    E621("https://e621.net/", "e621"),
    E926("https://e926.net/", "e926"),
}

data class ApiCredentials(
    val site: E621Site,
    val username: String,
    val apiKey: String,
)

class CredentialProvider {
    @Volatile
    private var credentials: ApiCredentials? = null

    fun set(credentials: ApiCredentials) {
        this.credentials = credentials
    }

    fun clear() {
        credentials = null
    }

    fun authorizationFor(site: E621Site): String? {
        val current = credentials ?: return null
        if (current.site != site) return null
        return Credentials.basic(current.username, current.apiKey, Charsets.UTF_8)
    }
}

class E621Network(
    private val credentialProvider: CredentialProvider,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(ApiRateLimitInterceptor())
        .addInterceptor(
            Interceptor { chain ->
                val request = chain.request()
                val site = E621Site.entries.firstOrNull {
                    request.url.host == it.baseUrl.removePrefix("https://").removeSuffix("/")
                }
                val builder = request.newBuilder().header("User-Agent", E430_USER_AGENT)
                site?.let(credentialProvider::authorizationFor)?.let { authorization ->
                    builder.header("Authorization", authorization)
                }
                chain.proceed(builder.build())
            },
        )
        .build()
    val mediaClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", E430_USER_AGENT).build())
        }
        .build()

    fun retrofit(site: E621Site): Retrofit = Retrofit.Builder()
        .baseUrl(site.baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

private class ApiRateLimitInterceptor : Interceptor {
    private val lock = Any()
    private var nextRequestAtNanos = 0L

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        synchronized(lock) {
            val waitNanos = nextRequestAtNanos - System.nanoTime()
            if (waitNanos > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(waitNanos)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("API request interrupted", interrupted)
                }
            }
            nextRequestAtNanos = System.nanoTime() + REQUEST_INTERVAL_NANOS
        }
        return chain.proceed(chain.request())
    }

    private companion object {
        val REQUEST_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1)
    }
}
