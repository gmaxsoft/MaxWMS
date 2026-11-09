package pl.maxwms.data.network

import okhttp3.Interceptor
import okhttp3.Response
import pl.maxwms.BuildConfig
import pl.maxwms.data.local.secure.SecureStore
import javax.inject.Inject

class ApiKeyInterceptor @Inject constructor(
    private val secureStore: SecureStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val key = secureStore.currentApiKey()
        val builder = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("User-Agent", "MaxWMS/${BuildConfig.VERSION_NAME}")
        if (key.isNotBlank()) {
            builder.header("Authorization", "Bearer $key")
            builder.header("X-Api-Key", key)
        }
        return chain.proceed(builder.build())
    }
}
