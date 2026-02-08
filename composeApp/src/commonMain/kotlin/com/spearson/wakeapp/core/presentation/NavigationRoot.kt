package com.spearson.wakeapp.core.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spearson.wakeapp.alarm_home.presentation.AlarmHomeRoot
import com.spearson.wakeapp.interval_alarm.presentation.IntervalAlarmRoot
import com.spearson.wakeapp.settings.presentation.SettingsScreen
import com.spearson.wakeapp.stats.presentation.StatsScreen

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentDestination.isTopLevelDestination()) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination.isDestinationSelected(destination.route),
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo<Route.Alarms> {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            modifier = Modifier.padding(paddingValues),
            navController = navController,
            startDestination = Route.Alarms,
        ) {
            composable<Route.Alarms> {
                AlarmHomeRoot(
                    onCreateAlarm = {
                        navController.navigate(Route.CreateAlarm)
                    },
                )
            }
            composable<Route.CreateAlarm> {
                IntervalAlarmRoot(
                    onClose = {
                        navController.navigateUp()
                    },
                )
            }
            composable<Route.Stats> {
                StatsScreen()
            }
            composable<Route.Settings> {
                SettingsScreen()
            }
        }
    }
}

private data class TopLevelDestination(
    val route: Route,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = Route.Alarms,
        label = "Alarms",
        icon = Icons.Default.Alarm,
    ),
    TopLevelDestination(
        route = Route.Stats,
        label = "Stats",
        icon = Icons.Default.BarChart,
    ),
    TopLevelDestination(
        route = Route.Settings,
        label = "Settings",
        icon = Icons.Default.Settings,
    ),
)

private fun NavDestination?.isTopLevelDestination(): Boolean {
    if (this == null) return false
    return hierarchy.any { destination ->
        destination.hasRoute<Route.Alarms>() ||
            destination.hasRoute<Route.Stats>() ||
            destination.hasRoute<Route.Settings>()
    }
}

private fun NavDestination?.isDestinationSelected(route: Route): Boolean {
    if (this == null) return false
    return when (route) {
        Route.Alarms -> hierarchy.any { it.hasRoute<Route.Alarms>() }
        Route.CreateAlarm -> hierarchy.any { it.hasRoute<Route.CreateAlarm>() }
        Route.Stats -> hierarchy.any { it.hasRoute<Route.Stats>() }
        Route.Settings -> hierarchy.any { it.hasRoute<Route.Settings>() }
    }
}
