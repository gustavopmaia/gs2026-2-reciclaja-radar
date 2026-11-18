package br.com.reciclaja.radar.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.reciclaja.radar.AppContainer
import br.com.reciclaja.radar.ui.rememberNotificationPermission
import br.com.reciclaja.radar.ui.screens.AlertDetailScreen
import br.com.reciclaja.radar.ui.screens.DashboardScreen
import br.com.reciclaja.radar.ui.screens.DashboardViewModel
import br.com.reciclaja.radar.ui.screens.DetailViewModel
import br.com.reciclaja.radar.ui.screens.HistoryScreen
import br.com.reciclaja.radar.ui.screens.HistoryViewModel
import br.com.reciclaja.radar.ui.screens.IndicatorDetailScreen
import br.com.reciclaja.radar.ui.screens.SettingsScreen
import br.com.reciclaja.radar.ui.screens.SettingsViewModel
import br.com.reciclaja.radar.ui.screens.factory

/** Rotas: só IDs trafegam na navegação, nunca objetos. */
object Routes {
    const val DASHBOARD = "painel"
    const val HISTORY = "historico"
    const val SETTINGS = "configuracoes"
    const val INDICATOR = "indicador/{territoryId}/{indicatorId}"
    const val ALERT = "alerta/{territoryId}/{indicatorId}"
    fun indicator(t: String, i: String) = "indicador/$t/$i"
    fun alert(t: String, i: String) = "alerta/$t/$i"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)
private val tabs = listOf(
    Tab(Routes.DASHBOARD, "Painel", Icons.Filled.Home),
    Tab(Routes.HISTORY, "Histórico", Icons.AutoMirrored.Filled.List),
    Tab(Routes.SETTINGS, "Ajustes", Icons.Filled.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarNavHost(container: AppContainer, deepLink: Pair<String, String>?, onDeepLinkConsumed: () -> Unit) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val factory = remember(container) { factory(container) }
    val dashboardVm: DashboardViewModel = viewModel(factory = factory)
    val permission = rememberNotificationPermission(container, onChanged = dashboardVm::onPermissionMaybeChanged)

    LaunchedEffect(deepLink) {
        deepLink?.let { (t, i) -> nav.navigate(Routes.alert(t, i)); onDeepLinkConsumed() }
    }

    val isTab = tabs.any { it.route == route }
    val title = when {
        route == Routes.INDICATOR -> "Detalhe do indicador"
        route == Routes.ALERT -> "Detalhe do alerta"
        route == Routes.HISTORY -> "Histórico de decisões"
        route == Routes.SETTINGS -> "Configurações"
        else -> "ReciclaJá Radar"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (!isTab) IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        bottomBar = {
            if (isTab) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label, maxLines = 1) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val ids = listOf(
            navArgument("territoryId") { type = NavType.StringType },
            navArgument("indicatorId") { type = NavType.StringType },
        )
        NavHost(nav, startDestination = Routes.DASHBOARD) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    vm = dashboardVm,
                    permission = permission,
                    padding = padding,
                    onIndicator = { t, i -> nav.navigate(Routes.indicator(t, i)) },
                    onAlert = { t, i -> nav.navigate(Routes.alert(t, i)) },
                )
            }
            composable(Routes.INDICATOR, arguments = ids) {
                val vm: DetailViewModel = viewModel(factory = factory)
                IndicatorDetailScreen(vm, padding, onAlert = { t, i -> nav.navigate(Routes.alert(t, i)) })
            }
            composable(Routes.ALERT, arguments = ids) {
                val vm: DetailViewModel = viewModel(factory = factory)
                AlertDetailScreen(vm, padding, snackbar)
            }
            composable(Routes.HISTORY) {
                val vm: HistoryViewModel = viewModel(factory = factory)
                HistoryScreen(vm, padding)
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(vm, permission, padding, snackbar)
            }
        }
    }
}
