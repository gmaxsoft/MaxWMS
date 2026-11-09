package pl.maxwms.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import pl.maxwms.domain.logic.BaseUrlNormalizer
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrmClientFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    @Volatile private var cachedUrl: String? = null
    @Volatile private var cached: CrmApiService? = null

    fun create(baseUrl: String): CrmApiService {
        val normalized = BaseUrlNormalizer.normalize(baseUrl)
            ?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Invalid CRM base URL")
        synchronized(this) {
            val existing = cached
            if (existing != null && cachedUrl == normalized) return existing
            val service = Retrofit.Builder()
                .baseUrl(normalized)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(CrmApiService::class.java)
            cached = service
            cachedUrl = normalized
            return service
        }
    }
}
