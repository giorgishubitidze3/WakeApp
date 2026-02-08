package com.spearson.wakeapp.interval_alarm.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spearson.wakeapp.core.theme.WakeAppTheme
import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import com.spearson.wakeapp.interval_alarm.presentation.util.toUiLabel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun IntervalAlarmScreen(
    state: IntervalAlarmState,
    onAction: (IntervalAlarmAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Header(
            onCancelClick = { onAction(IntervalAlarmAction.OnCancelClick) },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WindowCard(
                    startTime = state.startTime,
                    endTime = state.endTime,
                    onStartHourChanged = { onAction(IntervalAlarmAction.OnStartHourChanged(it)) },
                    onStartMinuteChanged = { onAction(IntervalAlarmAction.OnStartMinuteChanged(it)) },
                    onEndHourChanged = { onAction(IntervalAlarmAction.OnEndHourChanged(it)) },
                    onEndMinuteChanged = { onAction(IntervalAlarmAction.OnEndMinuteChanged(it)) },
                )
            }
            item {
                IntervalCard(
                    intervalMinutes = state.intervalMinutes,
                    onIntervalChanged = { onAction(IntervalAlarmAction.OnIntervalMinutesChanged(it)) },
                )
            }
            item {
                ToggleCard(
                    title = "Randomize Interval",
                    subtitle = "Fluctuates for lighter wake-up",
                    checked = state.randomizeInterval,
                    onCheckedChange = { onAction(IntervalAlarmAction.OnRandomizeIntervalChanged(it)) },
                )
            }
            item {
                SettingRowCard(
                    title = "Alarm Sound",
                    subtitle = "Morning Breeze",
                )
            }
            item {
                SettingRowCard(
                    title = "Haptics",
                    subtitle = "Gentle Pulse",
                )
            }
            item {
                DaysCard(
                    activeDays = state.activeDays,
                    onDayToggle = { onAction(IntervalAlarmAction.OnWeekdayToggle(it)) },
                )
            }
            item {
                PreviewCard(
                    alarmsPerDay = state.alarmsPerActiveDay,
                    totalPerWeek = state.totalAlarmsPerWeek,
                    previewTimes = state.previewTimes,
                )
            }
            if (state.statusMessage != null) {
                item {
                    Text(
                        text = state.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }
        Button(
            onClick = { onAction(IntervalAlarmAction.OnSavePlanClick) },
            enabled = !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(if (state.isSaving) "Saving..." else "Save Alarm")
        }
    }
}

@Composable
private fun Header(
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onCancelClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text("Cancel")
        }
        Text(
            text = "Set Smart Alarm",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Box(modifier = Modifier.width(88.dp))
    }
}

@Composable
private fun WindowCard(
    startTime: TimeOfDay,
    endTime: TimeOfDay,
    onStartHourChanged: (Int) -> Unit,
    onStartMinuteChanged: (Int) -> Unit,
    onEndHourChanged: (Int) -> Unit,
    onEndMinuteChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeWindowSummary(
                    title = "START WINDOW",
                    time = startTime,
                )
                Text(
                    text = ">",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TimeWindowSummary(
                    title = "END WINDOW",
                    time = endTime,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TimeAdjuster(
                    title = "Start",
                    time = startTime,
                    onHourChanged = onStartHourChanged,
                    onMinuteChanged = onStartMinuteChanged,
                )
                TimeAdjuster(
                    title = "End",
                    time = endTime,
                    onHourChanged = onEndHourChanged,
                    onMinuteChanged = onEndMinuteChanged,
                )
            }
        }
    }
}

@Composable
private fun TimeWindowSummary(
    title: String,
    time: TimeOfDay,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = time.toUiLabel(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TimeAdjuster(
    title: String,
    time: TimeOfDay,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberAdjuster(
                value = time.hour,
                min = 0,
                max = 23,
                onValueChanged = onHourChanged,
            )
            NumberAdjuster(
                value = time.minute,
                min = 0,
                max = 59,
                step = 5,
                onValueChanged = onMinuteChanged,
            )
        }
    }
}

@Composable
private fun NumberAdjuster(
    value: Int,
    min: Int,
    max: Int,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = { onValueChanged(cycle(value - step, min, max)) }) {
            Text("-")
        }
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = { onValueChanged(cycle(value + step, min, max)) }) {
            Text("+")
        }
    }
}

@Composable
private fun IntervalCard(
    intervalMinutes: Int,
    onIntervalChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Wake Interval",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$intervalMinutes minutes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = intervalMinutes.toFloat(),
                onValueChange = { onIntervalChanged(it.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("1M", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("10M", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun SettingRowCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DaysCard(
    activeDays: Set<Weekday>,
    onDayToggle: (Weekday) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Active Days",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Weekday.entries.forEach { weekday ->
                    FilterChip(
                        selected = weekday in activeDays,
                        onClick = { onDayToggle(weekday) },
                        label = { Text(weekday.shortLabel.take(1)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    alarmsPerDay: Int,
    totalPerWeek: Int,
    previewTimes: List<TimeOfDay>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$alarmsPerDay alarms per day - $totalPerWeek per week",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (previewTimes.isNotEmpty()) {
                Text(
                    text = previewTimes.take(4).joinToString("  ") { it.toUiLabel() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun cycle(value: Int, min: Int, max: Int): Int {
    if (value < min) return max
    if (value > max) return min
    return value
}

@Preview
@Composable
private fun IntervalAlarmScreenPreview() {
    WakeAppTheme {
        IntervalAlarmScreen(
            state = IntervalAlarmState(
                intervalMinutes = 5,
                alarmsPerActiveDay = 7,
                totalAlarmsPerWeek = 35,
                previewTimes = listOf(
                    TimeOfDay(7, 0),
                    TimeOfDay(7, 5),
                    TimeOfDay(7, 10),
                    TimeOfDay(7, 15),
                ),
            ),
            onAction = {},
        )
    }
}
