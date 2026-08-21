package com.secureguard.enterprise.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.SportsKabaddi
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.secureguard.enterprise.presentation.ui.actions.ActionsScreen
import com.secureguard.enterprise.presentation.ui.addasset.AddAssetScreen
import com.secureguard.enterprise.presentation.ui.agent.AgentConfigScreen
import com.secureguard.enterprise.presentation.ui.alerts.AlertsScreen
import com.secureguard.enterprise.presentation.ui.assets.AssetDetailScreen
import com.secureguard.enterprise.presentation.ui.assets.AssetListScreen
import com.secureguard.enterprise.presentation.ui.dashboard.DashboardScreen
import com.secureguard.enterprise.presentation.ui.map.MapScreen
import com.secureguard.enterprise.presentation.ui.settings.SettingsScreen
import com.secureguard.enterprise.presentation.ui.nodes.NodeStatusScreen
import com.secureguard.enterprise.presentation.ui.tempmail.TempMailScreen
import com.secureguard.enterprise.ui.optical.OpticalScanScreen
import com.secureguard.enterprise.ui.scan.QrScanScreen

object Routes {
    const val DASHBOARD    = "dashboard"
    const val ASSETS       = "assets"
    const val MAP          = "map"
    const val ACTIONS      = "actions"
    const val AGENT        = "agent"
    const val SETTINGS     = "settings"
    const val ALERTS       = "alerts"
    const val SCAN         = "scan"
    const val OPTICAL      = "optical"
    const val NODES        = "nodes"
    const val TEMPMAIL     = "tempmail"
    const val ADD_ASSET    = "add_asset?payload={payload}"
    fun addAssetWithPayload(payload: String): String =
        "add_asset?payload=${java.net.URLEncoder.encode(payload, "UTF-8")}"
    const val ADD_ASSET_PLAIN = "add_asset"
    const val ADD_ASSET_ARG   = "payload"
    const val ASSET_DETAIL  = "asset_detail/{assetId}"
    fun assetDetail(id: String) = "asset_detail/$id"
    const val ARG_ASSET_ID = "assetId"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    TabItem(Routes.DASHBOARD, "Dashboard", Icons.Outlined.Dashboard),
    TabItem(Routes.ASSETS,    "Assets",    Icons.Outlined.Pets),
    TabItem(Routes.MAP,       "Karte",     Icons.Outlined.Map),
    TabItem(Routes.ACTIONS,   "Aktionen",  Icons.Outlined.SportsKabaddi),
    TabItem(Routes.AGENT,     "Agent",     Icons.Outlined.SmartToy)
)

@Composable
fun SecureGuardNavHost() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isTabRoute = Tabs.any { it.route == currentRoute } ||
                     currentRoute == Routes.ASSET_DETAIL ||
                     currentRoute == Routes.SETTINGS ||
                     currentRoute == Routes.ALERTS ||
                     currentRoute?.startsWith("add_asset") == true

    Scaffold(
        bottomBar = {
            if (isTabRoute || currentRoute in listOf(Routes.DASHBOARD, Routes.ASSETS, Routes.MAP, Routes.ACTIONS, Routes.AGENT,
                                                     Routes.ASSET_DETAIL, Routes.ALERTS, Routes.SETTINGS) ||
                currentRoute?.startsWith("add_asset") == true) {
                NavigationBar {
                    Tabs.forEach { tab ->
                        val selected = backStack?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) { DashboardScreen() }
            composable(Routes.ASSETS)    { AssetListScreen() }
            composable(Routes.MAP)       { MapScreen() }
            composable(Routes.ACTIONS)   { ActionsScreen() }
            composable(Routes.AGENT)     { AgentConfigScreen() }
            composable(Routes.SETTINGS)  { SettingsScreen() }
            composable(Routes.ALERTS)    { AlertsScreen() }
            composable(Routes.SCAN)      { QrScanScreen() }
            composable(Routes.OPTICAL)   { OpticalScanScreen() }
            composable(Routes.NODES)     { NodeStatusScreen() }
            composable(Routes.TEMPMAIL)  { TempMailScreen() }

            composable(
                route = Routes.ASSET_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_ASSET_ID) { type = NavType.StringType })
            ) { entry: NavBackStackEntry ->
                val id = entry.arguments?.getString(Routes.ARG_ASSET_ID).orEmpty()
                AssetDetailScreen(assetId = id)
            }

            composable(
                route = Routes.ADD_ASSET,
                arguments = listOf(
                    navArgument(Routes.ADD_ASSET_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val payload = entry.arguments?.getString(Routes.ADD_ASSET_ARG)
                AddAssetScreen(scannedPayload = payload)
            }
            composable(Routes.ADD_ASSET_PLAIN) {
                AddAssetScreen(scannedPayload = null)
            }
        }
    }
}
