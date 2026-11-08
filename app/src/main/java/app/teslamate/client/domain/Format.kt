package app.teslamate.client.domain

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

fun formatTeslaModel(model: String): String {
    val trimmed = model.trim()
    if (trimmed.isEmpty()) return ""
    return when (trimmed.lowercase(Locale.US)) {
        "s", "3", "x", "y" -> "Model ${trimmed.uppercase(Locale.US)}"
        "cybertruck" -> "Cybertruck"
        "semi" -> "Semi"
        "roadster" -> "Roadster"
        else -> trimmed
    }
}

fun lengthUnitLabel(code: String): String = if (code.equals("mi", ignoreCase = true)) "mi" else "km"

fun tempUnitLabel(code: String): String = if (code.equals("F", ignoreCase = true)) "°F" else "°C"

fun speedUnitLabel(code: String): String = if (code.equals("mi", ignoreCase = true)) "mph" else "km/h"

fun consumptionUnitLabel(code: String): String = if (code.equals("mi", ignoreCase = true)) "Wh/mi" else "Wh/km"

fun pressureUnitLabel(code: String?): String = if (code.equals("psi", ignoreCase = true)) "psi" else "bar"

fun formatFixed(value: Double, decimals: Int, locale: Locale = Locale.getDefault()): String {
    return String.format(locale, "%.${decimals}f", value)
}

fun formatQuantity(value: Double, decimals: Int, unit: String, locale: Locale = Locale.getDefault()): String {
    return "${formatFixed(value, decimals, locale)} $unit"
}

fun formatApiDate(raw: String?, zone: ZoneId = ZoneId.systemDefault()): String {
    if (raw.isNullOrBlank()) return "—"
    val parsed = runCatching { OffsetDateTime.parse(raw) }.getOrNull() ?: return raw
    if (parsed.year < 2000) return "—"
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return parsed.atZoneSameInstant(zone).format(formatter)
}

fun formatDuration(durationStr: String, durationMin: Int): String {
    if (durationStr.isNotBlank()) return durationStr
    if (durationMin <= 0) return "—"
    val hours = durationMin / 60
    val minutes = durationMin % 60
    return "%02d:%02d".format(hours, minutes)
}

data class HourMinute(val hours: Int, val minutes: Int)

fun splitHours(hours: Double): HourMinute? {
    if (hours <= 0.0) return null
    val totalMinutes = (hours * 60.0).roundToInt().coerceAtLeast(1)
    return HourMinute(hours = totalMinutes / 60, minutes = totalMinutes % 60)
}

fun hasCoordinates(latitude: Double, longitude: Double): Boolean {
    return latitude != 0.0 || longitude != 0.0
}
