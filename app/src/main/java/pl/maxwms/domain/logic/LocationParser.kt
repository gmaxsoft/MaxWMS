package pl.maxwms.domain.logic

data class ParsedLocation(
    val zone: String,
    val rack: String,
)

object LocationParser {
    fun parse(location: String): ParsedLocation {
        val parts = location.split("->").map { it.trim() }.filter { it.isNotEmpty() }
        return ParsedLocation(
            zone = parts.getOrElse(0) { "" },
            rack = parts.getOrElse(1) { "" },
        )
    }
}
