package pl.maxwms.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class SyncFailure {
    INVALID_URL,
    UNAUTHORIZED,
    SERVER,
    NETWORK,
    FORMAT,
    UNKNOWN,
}

data class AppSettings(
    val baseUrl: String = "",
    val apiKey: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastSyncAt: Long? = null,
    val syncFailure: SyncFailure? = null,
    val companyName: String? = null,
    val seeded: Boolean = false,
)
