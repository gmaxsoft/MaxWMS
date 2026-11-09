package pl.maxwms.domain.logic

import java.security.MessageDigest

object ServiceCodeHasher {
    fun sha256(code: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(code.trim().toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun matches(code: String, expectedHash: String): Boolean {
        if (code.isBlank() || expectedHash.isBlank()) return false
        val actual = sha256(code)
        val expected = expectedHash.trim().lowercase()
        if (actual.length != expected.length) return false
        return MessageDigest.isEqual(actual.toByteArray(Charsets.UTF_8), expected.toByteArray(Charsets.UTF_8))
    }
}
