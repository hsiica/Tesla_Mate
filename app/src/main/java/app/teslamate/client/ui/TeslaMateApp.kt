package app.teslamate.client.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.teslamate.client.R
import app.teslamate.client.data.settings.SettingsRepository
import app.teslamate.client.ui.charges.ChargeDetailScreen
import app.teslamate.client.ui.charges.ChargesScreen
import app.teslamate.client.ui.components.ScreenLoading
import app.teslamate.client.ui.drives.DriveDetailScreen
import app.teslamate.client.ui.drives.DrivesScreen
import app.teslamate.client.ui.home.GarageState
import app.teslamate.client.ui.home.GarageViewModel
import app.teslamate.client.ui.home.StatusScreen
import app.teslamate.client.ui.home.StatusViewModel
import app.teslamate.client.ui.settings.SettingsScreen
import app.teslamate.client.ui.theme.TeslaMateTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private object Routes {
    const val STATUS = "status"
    const val DRIVES = "drives"
    const val CHARGES = "charges"
    const val SETTINGS = "settings"
    const val DRIVE_DETAIL = "drive/{carId}/{driveId}"
    const val CHARGE_DETAIL = "charge/{carId}/{chargeId}"

    fun drive(carId: Int, driveId: Int) = "drive/$carId/$driveId"
    fun charge(carId: Int, chargeId: Int) = "charge/$carId/$chargeId"
}

@HiltViewModel
class RootViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val configured = settingsRepository.settings
        .map { it.baseUrl.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun TeslaMateRoot(viewModel: RootViewModel = hiltViewModel()) {
    val configured by viewModel.configured.collectAsStateWithLifecycle()
    TeslaMateTheme {
        when (configured) {
            null -> ScreenLoading()
            false -> SettingsScreen(onboarding = true)
            true -> MainScaffold()
        }
    }
}

@Composable
private fun MainScaffold() {
    val nav = rememberNavController()
    val garage: GarageViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        garage.refresh(initial = garage.state.value !is GarageState.Ready)
    }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(
        Tab(Routes.STATUS, R.string.nav_status, Icons.Filled.DirectionsCar),
        Tab(Routes.DRIVES, R.string.nav_drives, Icons.Filled.Route),
        Tab(Routes.CHARGES, R.string.nav_charges, Icons.Filled.Bolt),
        Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
    )
    val selected = when {
        route == Routes.DRIVES || route?.startsWith("drive/") == true -> Routes.DRIVES
        route == Routes.CHARGES || route?.startsWith("charge/") == true -> Routes.CHARGES
        route == Routes.SETTINGS -> Routes.SETTINGS
        else -> Routes.STATUS
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.label)) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.STATUS,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.STATUS) {
                val statusViewModel: StatusViewModel = hiltViewModel()
                StatusScreen(garageViewModel = garage, statusViewModel = statusViewModel)
            }
            composable(Routes.DRIVES) {
                DrivesScreen(
                    garageViewModel = garage,
                    onOpenDrive = { carId, driveId -> nav.navigate(Routes.drive(carId, driveId)) },
                )
            }
            composable(Routes.CHARGES) {
                ChargesScreen(
                    garageViewModel = garage,
                    onOpenCharge = { carId, chargeId -> nav.navigate(Routes.charge(carId, chargeId)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onboarding = false, embedded = true)
            }
            composable(
                route = Routes.DRIVE_DETAIL,
                arguments = listOf(
                    navArgument("carId") { type = NavType.IntType },
                    navArgument("driveId") { type = NavType.IntType },
                ),
            ) {
                DriveDetailScreen(onBack = { nav.popBackStack() })
            }
            composable(
                route = Routes.CHARGE_DETAIL,
                arguments = listOf(
                    navArgument("carId") { type = NavType.IntType },
                    navArgument("chargeId") { type = NavType.IntType },
                ),
            ) {
                ChargeDetailScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)
