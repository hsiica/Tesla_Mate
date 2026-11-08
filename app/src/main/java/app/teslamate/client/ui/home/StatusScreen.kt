package app.teslamate.client.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.teslamate.client.R
import app.teslamate.client.domain.CarStatus
import app.teslamate.client.domain.formatQuantity
import app.teslamate.client.domain.formatTeslaModel
import app.teslamate.client.ui.chargingStateLabel
import app.teslamate.client.ui.components.CarMenu
import app.teslamate.client.ui.components.InfoRow
import app.teslamate.client.ui.components.ScreenLoading
import app.teslamate.client.ui.components.ScreenMessage
import app.teslamate.client.ui.components.SectionCard
import app.teslamate.client.ui.coordinateLabel
import app.teslamate.client.ui.dateLabel
import app.teslamate.client.ui.hoursLabel
import app.teslamate.client.ui.lengthLabel
import app.teslamate.client.ui.message
import app.teslamate.client.ui.pressureLabel
import app.teslamate.client.ui.speedLabel
import app.teslamate.client.ui.tempLabel
import app.teslamate.client.ui.vehicleStateLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatusScreen(
    garageViewModel: GarageViewModel,
    statusViewModel: StatusViewModel,
    modifier: Modifier = Modifier,
) {
    val garage by garageViewModel.state.collectAsStateWithLifecycle()
    val refreshingCars by garageViewModel.refreshing.collectAsStateWithLifecycle()
    val statusUi by statusViewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(statusViewModel) {
        statusViewModel.toasts.collect { snackbar.showSnackbar(it.message(context)) }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            val ready = garage as? GarageState.Ready
            TopAppBar(
                title = {
                    if (ready != null) {
                        CarMenu(
                            cars = ready.cars,
                            selectedId = ready.selectedId,
                            onSelect = garageViewModel::selectCar,
                        )
                    } else {
                        Text(stringResource(R.string.nav_status))
                    }
                },
            )
        },
    ) { padding ->
        when (val state = garage) {
            GarageState.Loading -> ScreenLoading(Modifier.padding(padding))
            GarageState.Empty -> ScreenMessage(
                title = stringResource(R.string.empty_cars_title),
                body = stringResource(R.string.empty_cars_body),
                actionLabel = stringResource(R.string.retry),
                onAction = { garageViewModel.refresh(initial = true) },
                modifier = Modifier.padding(padding),
            )
            is GarageState.Error -> ScreenMessage(
                title = stringResource(R.string.nav_status),
                body = state.error.message(context),
                actionLabel = stringResource(R.string.retry),
                onAction = { garageViewModel.refresh(initial = true) },
                modifier = Modifier.padding(padding),
            )
            is GarageState.Ready -> PullToRefreshBox(
                isRefreshing = refreshingCars || statusUi.refreshing,
                onRefresh = {
                    garageViewModel.refresh()
                    statusViewModel.refresh()
                },
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                when {
                    statusUi.loading && statusUi.status == null -> ScreenLoading()
                    statusUi.error != null && statusUi.status == null -> ScreenMessage(
                        title = stringResource(R.string.nav_status),
                        body = statusUi.error!!.message(context),
                        actionLabel = stringResource(R.string.retry),
                        onAction = statusViewModel::refresh,
                    )
                    statusUi.status != null -> StatusBody(statusUi.status!!)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusBody(status: CarStatus) {
    val dash = stringResource(R.string.dash)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(vehicleStateLabel(status.state), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    val since = dateLabel(status.stateSince)
                    if (since != dash) {
                        Text(
                            stringResource(R.string.since, since),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.percent_value, status.batteryLevel),
                        style = MaterialTheme.typography.displayMedium,
                    )
                    if (status.usableBatteryLevel != status.batteryLevel) {
                        Text(
                            stringResource(R.string.usable_battery, status.usableBatteryLevel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { status.batteryLevel.coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    InfoRow(stringResource(R.string.rated_range), lengthLabel(status.ratedRange, status.units.length, 0))
                    InfoRow(stringResource(R.string.est_range), lengthLabel(status.estRange, status.units.length, 0))
                    InfoRow(stringResource(R.string.ideal_range), lengthLabel(status.idealRange, status.units.length, 0))
                }
            }
        }
        if (!status.healthy) {
            item {
                Text(stringResource(R.string.logger_unhealthy), color = MaterialTheme.colorScheme.error)
            }
        }
        if (status.state.equals("asleep", ignoreCase = true)) {
            item {
                Text(
                    stringResource(R.string.asleep_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(stringResource(R.string.charging_section)) {
                InfoRow(
                    stringResource(R.string.charging_section),
                    if (status.pluggedIn) stringResource(R.string.plugged_in) else stringResource(R.string.not_plugged),
                )
                if (status.chargingState.isNotBlank()) {
                    InfoRow(stringResource(R.string.state_charging), chargingStateLabel(status.chargingState))
                }
                if (status.pluggedIn || status.chargerPowerKw > 0.0) {
                    InfoRow(stringResource(R.string.charger_power), formatQuantity(status.chargerPowerKw, 1, "kW"))
                    InfoRow(stringResource(R.string.energy_added), formatQuantity(status.chargeEnergyAdded, 1, "kWh"))
                    InfoRow(stringResource(R.string.charge_limit), stringResource(R.string.percent_value, status.chargeLimitSoc))
                    InfoRow(stringResource(R.string.time_to_full), hoursLabel(status.timeToFullHours))
                    if (status.chargePortOpen) {
                        Text(stringResource(R.string.charge_port_open), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.climate_section)) {
                InfoRow(stringResource(R.string.inside_temp), tempLabel(status.insideTemp, status.units.temperature))
                InfoRow(stringResource(R.string.outside_temp), tempLabel(status.outsideTemp, status.units.temperature))
                InfoRow(
                    stringResource(R.string.climate_section),
                    stringResource(if (status.climateOn) R.string.climate_on else R.string.climate_off),
                )
                if (status.preconditioning) {
                    Text(stringResource(R.string.preconditioning))
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.vehicle_section)) {
                val model = listOf(formatTeslaModel(status.model), status.trim).filter { it.isNotBlank() }.joinToString(" ")
                if (model.isNotBlank()) InfoRow(stringResource(R.string.model_label), model)
                InfoRow(stringResource(R.string.odometer), lengthLabel(status.odometer, status.units.length, 1))
                InfoRow(
                    stringResource(R.string.software),
                    status.softwareVersion.ifBlank { dash },
                )
                if (status.updateAvailable && status.updateVersion.isNotBlank()) {
                    Text(stringResource(R.string.update_available, status.updateVersion))
                }
                InfoRow(
                    stringResource(R.string.location),
                    status.geofence.ifBlank { dash },
                )
                InfoRow(stringResource(R.string.coordinates), coordinateLabel(status.latitude, status.longitude))
                InfoRow(
                    stringResource(R.string.locked),
                    stringResource(if (status.locked) R.string.locked else R.string.unlocked),
                )
                InfoRow(
                    stringResource(R.string.sentry_on),
                    stringResource(if (status.sentryMode) R.string.sentry_on else R.string.sentry_off),
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (status.windowsOpen) SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.windows_open)) })
                    if (status.doorsOpen) SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.doors_open)) })
                    if (status.trunkOpen) SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.trunk_open)) })
                    if (status.frunkOpen) SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.frunk_open)) })
                    if (status.userPresent) SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.user_present)) })
                }
            }
        }
        if (status.speed > 0 || status.shiftState.isNotBlank()) {
            item {
                SectionCard(stringResource(R.string.driving_section)) {
                    InfoRow(stringResource(R.string.shift), status.shiftState.ifBlank { dash })
                    InfoRow(stringResource(R.string.speed), speedLabel(status.speed.toDouble(), status.units.length))
                    InfoRow(stringResource(R.string.power), formatQuantity(status.powerKw.toDouble(), 0, "kW"))
                    InfoRow(stringResource(R.string.elevation), formatQuantity(status.elevation.toDouble(), 0, "m"))
                }
            }
        }
        if (status.tpmsFl > 0 || status.tpmsFr > 0 || status.tpmsRl > 0 || status.tpmsRr > 0) {
            item {
                SectionCard(stringResource(R.string.tpms)) {
                    InfoRow(stringResource(R.string.tpms_fl), pressureLabel(status.tpmsFl, status.units.pressure))
                    InfoRow(stringResource(R.string.tpms_fr), pressureLabel(status.tpmsFr, status.units.pressure))
                    InfoRow(stringResource(R.string.tpms_rl), pressureLabel(status.tpmsRl, status.units.pressure))
                    InfoRow(stringResource(R.string.tpms_rr), pressureLabel(status.tpmsRr, status.units.pressure))
                }
            }
        }
    }
}
