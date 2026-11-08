package app.teslamate.client.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.teslamate.client.R
import app.teslamate.client.domain.HourMinute
import app.teslamate.client.domain.formatApiDate
import app.teslamate.client.domain.formatDuration
import app.teslamate.client.domain.formatFixed
import app.teslamate.client.domain.formatQuantity
import app.teslamate.client.domain.hasCoordinates
import app.teslamate.client.domain.lengthUnitLabel
import app.teslamate.client.domain.pressureUnitLabel
import app.teslamate.client.domain.speedUnitLabel
import app.teslamate.client.domain.splitHours
import app.teslamate.client.domain.tempUnitLabel

@Composable
fun vehicleStateLabel(raw: String): String = when (raw.trim().lowercase()) {
    "online" -> stringResource(R.string.state_online)
    "asleep" -> stringResource(R.string.state_asleep)
    "offline" -> stringResource(R.string.state_offline)
    "charging" -> stringResource(R.string.state_charging)
    "driving" -> stringResource(R.string.state_driving)
    "suspended" -> stringResource(R.string.state_suspended)
    "updating" -> stringResource(R.string.state_updating)
    "" -> stringResource(R.string.state_unknown)
    else -> raw
}

@Composable
fun chargingStateLabel(raw: String): String = when (raw.trim().lowercase()) {
    "charging" -> stringResource(R.string.charging_state_charging)
    "complete" -> stringResource(R.string.charging_state_complete)
    "stopped" -> stringResource(R.string.charging_state_stopped)
    "disconnected" -> stringResource(R.string.charging_state_disconnected)
    "nopower" -> stringResource(R.string.charging_state_nopower)
    "starting" -> stringResource(R.string.charging_state_starting)
    "" -> stringResource(R.string.dash)
    else -> raw
}

@Composable
fun placeLabel(raw: String): String = raw.ifBlank { stringResource(R.string.unknown_place) }

@Composable
fun dateLabel(raw: String): String {
    val formatted = formatApiDate(raw)
    return if (formatted == "—") stringResource(R.string.dash) else formatted
}

@Composable
fun lengthLabel(value: Double, unitCode: String, decimals: Int = 1): String =
    formatQuantity(value, decimals, lengthUnitLabel(unitCode))

@Composable
fun tempLabel(value: Double, unitCode: String): String =
    formatQuantity(value, 1, tempUnitLabel(unitCode))

@Composable
fun speedLabel(value: Double, unitCode: String): String =
    formatQuantity(value, if (value % 1.0 == 0.0) 0 else 1, speedUnitLabel(unitCode))

@Composable
fun durationLabel(durationStr: String, durationMin: Int): String {
    val formatted = formatDuration(durationStr, durationMin)
    return if (formatted == "—") stringResource(R.string.dash) else formatted
}

@Composable
fun hoursLabel(hours: Double): String {
    val parts: HourMinute = splitHours(hours) ?: return stringResource(R.string.dash)
    return if (parts.hours > 0) {
        stringResource(R.string.hours_minutes, parts.hours, parts.minutes)
    } else {
        stringResource(R.string.minutes_only, parts.minutes)
    }
}

@Composable
fun coordinateLabel(latitude: Double, longitude: Double): String {
    if (!hasCoordinates(latitude, longitude)) return stringResource(R.string.dash)
    return "${formatFixed(latitude, 5)}, ${formatFixed(longitude, 5)}"
}

@Composable
fun pressureLabel(value: Double, unitCode: String?): String =
    formatQuantity(value, 2, pressureUnitLabel(unitCode))
