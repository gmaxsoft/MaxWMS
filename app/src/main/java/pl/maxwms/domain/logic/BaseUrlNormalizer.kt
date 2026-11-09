package pl.maxwms.domain.logic

object BaseUrlNormalizer {
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.any { it.isWhitespace() }) return null
        val withScheme = when {
            trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
            else -> "https://$trimmed"
        }
        val host = withScheme.substringAfter("://", missingDelimiterValue = "")
        if (host.isBlank() || host.startsWith("/")) return null
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}
