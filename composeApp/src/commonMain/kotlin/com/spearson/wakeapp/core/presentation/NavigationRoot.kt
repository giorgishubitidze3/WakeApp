package com.spearson.wakeapp.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spearson.wakeapp.interval_alarm.presentation.IntervalAlarmRoot

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Route.IntervalAlarm,
    ) {
        composable<Route.IntervalAlarm> {
            IntervalAlarmRoot()
        }
    }
}
