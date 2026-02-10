package com.spearson.wakeapp.alarm_home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spearson.wakeapp.core.theme.WakeAppTheme
import com.spearson.wakeapp.interval_alarm.domain.model.HapticsPattern
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import com.spearson.wakeapp.interval_alarm.presentation.util.toUiLabel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AlarmHomeScreen(
    state: AlarmHomeState,
    onAction: (AlarmHomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pendingDeletePlan = state.plans.firstOrNull { it.id == state.pendingDeletePlanId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Alarms",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = { onAction(AlarmHomeAction.OnCreateAlarmClick) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create alarm",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        if (state.plans.isEmpty() && !state.isLoading) {
            EmptyPlansMessage()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.plans,
                    key = { it.id },
                ) { plan ->
                    AlarmPlanRow(
                        plan = plan,
                        onClick = {
                            onAction(AlarmHomeAction.OnPlanClick(plan.id))
                        },
                        onLongClick = {
                            onAction(AlarmHomeAction.OnPlanLongPress(plan.id))
                        },
                        onEnabledChanged = { isEnabled ->
                            onAction(
                                AlarmHomeAction.OnPlanEnabledChanged(
                                    planId = plan.id,
                                    isEnabled = isEnabled,
                                ),
                            )
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
        if (state.statusMessage != null) {
            Text(
                text = state.statusMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (pendingDeletePlan != null) {
        AlertDialog(
            onDismissRequest = { onAction(AlarmHomeAction.OnDeletePlanDismiss) },
            title = {
                Text(
                    text = "Delete alarm?",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = "Remove ${pendingDeletePlan.startTime.toUiLabel()} interval alarm plan.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            dismissButton = {
                TextButton(onClick = { onAction(AlarmHomeAction.OnDeletePlanDismiss) }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = { onAction(AlarmHomeAction.OnDeletePlanConfirm) }) {
                    Text("Delete")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmPlanRow(
    plan: IntervalAlarmPlan,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = plan.startTime.toClockDisplay(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize * 2f,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = plan.startTime.toAmPm(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
                if (plan.selectedHapticsEnabled()) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Haptics enabled",
                        tint = HapticsIconColor,
                        modifier = Modifier
                            .padding(bottom = 4.dp, start = 2.dp)
                            .size(16.dp),
                    )
                }
            }
            Text(
                text = "Every ${plan.intervalMinutes} min until ${plan.endTime.toUiLabel()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Weekday.entries.forEach { weekday ->
                    val isActive = weekday in plan.activeDays
                    Text(
                        text = weekday.shortLabel.take(1),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        },
                    )
                }
            }
        }
        AlarmEnabledToggle(
            checked = plan.isEnabled,
            onCheckedChange = onEnabledChanged,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun AlarmEnabledToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = ToggleThumb,
            uncheckedThumbColor = ToggleThumb,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedTrackColor = ToggleOffTrack,
            checkedBorderColor = Color.Transparent,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun EmptyPlansMessage(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "No smart alarms yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Tap + to create your first interval alarm.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay.toClockDisplay(): String {
    val hour12 = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    val minutes = minute.toString().padStart(2, '0')
    return "$hour12:$minutes"
}

private fun com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay.toAmPm(): String {
    return if (hour < 12) "AM" else "PM"
}

private fun IntervalAlarmPlan.selectedHapticsEnabled(): Boolean {
    return hapticsPattern != HapticsPattern.NoneOff
}

private val HomeBackground = Color(0xFF0A0A0A)
private val ToggleOffTrack = Color(0xFF1E293B)
private val ToggleThumb = Color(0xFFFFFFFF)
private val HapticsIconColor = Color(0xFFF59E0B)

@Preview
@Composable
private fun AlarmHomeScreenPreview() {
    WakeAppTheme {
        AlarmHomeScreen(
            state = AlarmHomeState(
                plans = listOf(
                    IntervalAlarmPlan(
                        id = "plan_1",
                        intervalMinutes = 2,
                        isEnabled = true,
                    ),
                    IntervalAlarmPlan(
                        id = "plan_2",
                        startTime = com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay(8, 15),
                        endTime = com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay(8, 30),
                        intervalMinutes = 5,
                        activeDays = setOf(Weekday.Monday, Weekday.Wednesday, Weekday.Friday),
                        isEnabled = true,
                    ),
                    IntervalAlarmPlan(
                        id = "plan_3",
                        startTime = com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay(10, 0),
                        endTime = com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay(10, 15),
                        intervalMinutes = 1,
                        activeDays = setOf(Weekday.Saturday, Weekday.Sunday),
                        isEnabled = false,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
