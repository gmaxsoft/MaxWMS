package pl.maxwms.presentation.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Context.findActivity(): Activity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    error("Brak aktywności")
}

private val polishDateTime: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("pl-PL"))

fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(polishDateTime)

fun formatDelta(delta: Int): String = if (delta > 0) "+$delta" else delta.toString()
