package app.teslamate.client.ui.drives

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
import app.teslamate.client.domain.DriveSummary
import app.teslamate.client.domain.Units
import app.teslamate.client.domain.consumptionUnitLabel
import app.teslamate.client.domain.formatQuantity
import app.teslamate.client.ui.components.CarMenu
import app.teslamate.client.ui.components.InfoRow
import app.teslamate.client.ui.components.ScreenLoading
import app.teslamate.client.ui.components.ScreenMessage
import app.teslamate.client.ui.components.SectionCard
import app.teslamate.client.ui.dateLabel
import app.teslamate.client.ui.durationLabel
import app.teslamate.client.ui.home.GarageState
import app.teslamate.client.ui.home.GarageViewModel
import app.teslamate.client.ui.lengthLabel
import app.teslamate.client.ui.message
import app.teslamate.client.ui.placeLabel
import app.teslamate.client.ui.speedLabel
import app.teslamate.client.ui.tempLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivesScreen(
    garageViewModel: GarageViewModel,
    onOpenDrive: (carId: Int, driveId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DrivesViewModel = hiltViewModel(),
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
                        Text(stringResource(R.string.drives_title))
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
                title = stringResource(R.string.drives_title),
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
                        title = stringResource(R.string.drives_title),
                        body = ui.error!!.message(context),
                        actionLabel = stringResource(R.string.retry),
                        onAction = viewModel::refresh,
                    )
                    ui.items.isNullOrEmpty() -> ScreenMessage(
                        title = stringResource(R.string.drives_title),
                        body = stringResource(R.string.empty_drives),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(ui.items.orEmpty(), key = { it.id }) { drive ->
                            DriveRow(drive, ui.units) { onOpenDrive(state.selectedId, drive.id) }
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
private fun DriveRow(drive: DriveSummary, units: Units, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(dateLabel(drive.startDate), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.arrow_places, placeLabel(drive.startAddress), placeLabel(drive.endAddress)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                listOf(
                    lengthLabel(drive.distance, units.length, 1),
                    durationLabel(drive.durationStr, drive.durationMin),
                    drive.energyConsumedNet?.let { formatQuantity(it, 1, "kWh") },
                ).filterNotNull().joinToString(" · "),
            )
            Text(stringResource(R.string.battery_arrow, drive.startBattery, drive.endBattery))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DriveDetailViewModel = hiltViewModel(),
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
                title = { Text(stringResource(R.string.drive_detail_title)) },
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
                title = stringResource(R.string.drive_detail_title),
                body = error!!.message(context),
                actionLabel = stringResource(R.string.retry),
                onAction = viewModel::refresh,
                modifier = Modifier.padding(padding),
            )
            detail != null -> {
                val drive = detail!!.drive
                val units = detail!!.units
                LazyColumn(
                    Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SectionCard(stringResource(R.string.drive_detail_title)) {
                            InfoRow(stringResource(R.string.start_place), placeLabel(drive.startAddress))
                            InfoRow(stringResource(R.string.end_place), placeLabel(drive.endAddress))
                            InfoRow(stringResource(R.string.distance), lengthLabel(drive.distance, units.length, 1))
                            InfoRow(stringResource(R.string.duration), durationLabel(drive.durationStr, drive.durationMin))
                            InfoRow(stringResource(R.string.time_range), "${dateLabel(drive.startDate)} – ${dateLabel(drive.endDate)}")
                            drive.energyConsumedNet?.let {
                                InfoRow(stringResource(R.string.energy_consumed), formatQuantity(it, 2, "kWh"))
                            }
                            drive.consumptionNet?.let {
                                InfoRow(
                                    stringResource(R.string.consumption),
                                    formatQuantity(it, 0, consumptionUnitLabel(units.length)),
                                )
                            }
                            InfoRow(stringResource(R.string.speed_max), speedLabel(drive.speedMax.toDouble(), units.length))
                            InfoRow(stringResource(R.string.speed_avg), speedLabel(drive.speedAvg, units.length))
                            InfoRow(stringResource(R.string.power_max), formatQuantity(drive.powerMax.toDouble(), 0, "kW"))
                            InfoRow(stringResource(R.string.power_min), formatQuantity(drive.powerMin.toDouble(), 0, "kW"))
                            InfoRow(
                                stringResource(R.string.battery_change),
                                stringResource(R.string.battery_arrow, drive.startBattery, drive.endBattery),
                            )
                            InfoRow(
                                stringResource(R.string.range_rated_change),
                                stringResource(
                                    R.string.range_arrow,
                                    lengthLabel(drive.ratedRangeStart, units.length, 0),
                                    lengthLabel(drive.ratedRangeEnd, units.length, 0),
                                ),
                            )
                            drive.outsideTemp?.let {
                                InfoRow(stringResource(R.string.outside_temp), tempLabel(it, units.temperature))
                            }
                            drive.insideTemp?.let {
                                InfoRow(stringResource(R.string.inside_temp), tempLabel(it, units.temperature))
                            }
                            if (detail!!.trackPointCount > 0) {
                                Text(stringResource(R.string.track_points, detail!!.trackPointCount))
                            }
                        }
                    }
                }
            }
        }
    }
}
