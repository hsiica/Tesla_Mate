package app.teslamate.client.ui.charges

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.teslamate.client.R
import app.teslamate.client.domain.ChargeSummary
import app.teslamate.client.domain.Units
import app.teslamate.client.domain.formatFixed
import app.teslamate.client.domain.formatQuantity
import app.teslamate.client.ui.components.CarMenu
import app.teslamate.client.ui.components.InfoRow
import app.teslamate.client.ui.components.ScreenLoading
import app.teslamate.client.ui.components.ScreenMessage
import app.teslamate.client.ui.components.SectionCard
import app.teslamate.client.ui.coordinateLabel
import app.teslamate.client.ui.dateLabel
import app.teslamate.client.ui.durationLabel
import app.teslamate.client.ui.home.GarageState
import app.teslamate.client.ui.home.GarageViewModel
import app.teslamate.client.ui.lengthLabel
import app.teslamate.client.ui.message
import app.teslamate.client.ui.placeLabel
import app.teslamate.client.ui.tempLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargesScreen(
    garageViewModel: GarageViewModel,
    onOpenCharge: (carId: Int, chargeId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChargesViewModel = hiltViewModel(),
) {
    val garage by garageViewModel.state.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.toasts.collect { snackbar.showSnackbar(it.message(context)) }
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
                        CarMenu(ready.cars, ready.selectedId, onSelect = garageViewModel::selectCar)
                    } else {
                        Text(stringResource(R.string.charges_title))
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
                modifier = Modifier.padding(padding),
            )
            is GarageState.Error -> ScreenMessage(
                title = stringResource(R.string.charges_title),
                body = state.error.message(context),
                actionLabel = stringResource(R.string.retry),
                onAction = { garageViewModel.refresh(initial = true) },
                modifier = Modifier.padding(padding),
            )
            is GarageState.Ready -> PullToRefreshBox(
                isRefreshing = ui.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                when {
                    ui.loading && ui.items == null -> ScreenLoading()
                    ui.error != null && ui.items == null -> ScreenMessage(
                        title = stringResource(R.string.charges_title),
                        body = ui.error!!.message(context),
                        actionLabel = stringResource(R.string.retry),
                        onAction = viewModel::refresh,
                    )
                    ui.items.isNullOrEmpty() -> ScreenMessage(
                        title = stringResource(R.string.charges_title),
                        body = stringResource(R.string.empty_charges),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(ui.items.orEmpty(), key = { it.id }) { charge ->
                            ChargeRow(charge, ui.units) { onOpenCharge(state.selectedId, charge.id) }
                        }
                        if (!ui.endReached) {
                            item {
                                TextButton(
                                    onClick = viewModel::loadMore,
                                    enabled = !ui.loadingMore,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(if (ui.loadingMore) R.string.loading else R.string.load_more))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChargeRow(charge: ChargeSummary, units: Units, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(dateLabel(charge.startDate), style = MaterialTheme.typography.titleMedium)
            Text(placeLabel(charge.address), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                listOf(
                    formatQuantity(charge.energyAdded, 1, "kWh"),
                    durationLabel(charge.durationStr, charge.durationMin),
                    formatFixed(charge.cost, 2),
                ).joinToString(" · "),
            )
            Text(stringResource(R.string.battery_arrow, charge.startBattery, charge.endBattery))
            Text(
                lengthLabel(charge.odometer, units.length, 0),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargeDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChargeDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.charge_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading && detail == null -> ScreenLoading(Modifier.padding(padding))
            error != null && detail == null -> ScreenMessage(
                title = stringResource(R.string.charge_detail_title),
                body = error!!.message(context),
                actionLabel = stringResource(R.string.retry),
                onAction = viewModel::refresh,
                modifier = Modifier.padding(padding),
            )
            detail != null -> {
                val charge = detail!!.charge
                val units = detail!!.units
                LazyColumn(
                    Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    item {
                        SectionCard(stringResource(R.string.charge_detail_title)) {
                            InfoRow(stringResource(R.string.address), placeLabel(charge.address))
                            InfoRow(stringResource(R.string.time_range), "${dateLabel(charge.startDate)} – ${dateLabel(charge.endDate)}")
                            InfoRow(stringResource(R.string.duration), durationLabel(charge.durationStr, charge.durationMin))
                            InfoRow(stringResource(R.string.energy_added), formatQuantity(charge.energyAdded, 2, "kWh"))
                            InfoRow(stringResource(R.string.energy_used), formatQuantity(charge.energyUsed, 2, "kWh"))
                            InfoRow(stringResource(R.string.cost), formatFixed(charge.cost, 2))
                            Text(
                                stringResource(R.string.cost_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            InfoRow(
                                stringResource(R.string.battery_change),
                                stringResource(R.string.battery_arrow, charge.startBattery, charge.endBattery),
                            )
                            InfoRow(
                                stringResource(R.string.range_rated_change),
                                stringResource(
                                    R.string.range_arrow,
                                    lengthLabel(charge.ratedRangeStart, units.length, 0),
                                    lengthLabel(charge.ratedRangeEnd, units.length, 0),
                                ),
                            )
                            InfoRow(stringResource(R.string.outside_temp), tempLabel(charge.outsideTemp, units.temperature))
                            InfoRow(stringResource(R.string.odometer), lengthLabel(charge.odometer, units.length, 1))
                            InfoRow(stringResource(R.string.coordinates), coordinateLabel(charge.latitude, charge.longitude))
                            detail!!.chargerPeak?.let { peak ->
                                InfoRow(stringResource(R.string.charger_power), formatQuantity(peak.powerKw.toDouble(), 0, "kW"))
                                InfoRow(
                                    stringResource(R.string.supply_label),
                                    stringResource(R.string.charger_detail, peak.voltage, peak.current, peak.phases),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
