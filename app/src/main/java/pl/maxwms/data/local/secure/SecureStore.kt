package pl.maxwms.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = openPrefs(context)

    private val unlockedFlow = MutableStateFlow(prefs.getBoolean(KEY_UNLOCKED, false))
    private val companyFlow = MutableStateFlow(prefs.getString(KEY_COMPANY, null))
    private val apiKeyFlow = MutableStateFlow(prefs.getString(KEY_API, "").orEmpty())

    val crmUnlocked: StateFlow<Boolean> = unlockedFlow.asStateFlow()
    val companyName: StateFlow<String?> = companyFlow.asStateFlow()
    val apiKey: StateFlow<String> = apiKeyFlow.asStateFlow()

    fun currentApiKey(): String = apiKeyFlow.value

    fun setCrmUnlocked(unlocked: Boolean, companyName: String?) {
        prefs.edit()
            .putBoolean(KEY_UNLOCKED, unlocked)
            .putString(KEY_COMPANY, companyName)
            .apply()
        unlockedFlow.value = unlocked
        companyFlow.value = companyName
    }

    fun setApiKey(value: String) {
        val trimmed = value.trim()
        prefs.edit().putString(KEY_API, trimmed).apply()
        apiKeyFlow.value = trimmed
    }

    private fun openPrefs(context: Context): SharedPreferences {
        return try {
            @Suppress("DEPRECATION")
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            @Suppress("DEPRECATION")
            EncryptedSharedPreferences.create(
                context,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            context.getSharedPreferences(FILE_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    private companion object {
        const val FILE = "maxwms_secure"
        const val FILE_FALLBACK = "maxwms_secure_fallback"
        const val KEY_UNLOCKED = "crm_unlocked"
        const val KEY_COMPANY = "company_name"
        const val KEY_API = "api_key"
    }
}
