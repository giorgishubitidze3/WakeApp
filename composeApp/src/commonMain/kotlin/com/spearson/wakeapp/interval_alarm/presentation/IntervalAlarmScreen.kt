package com.spearson.wakeapp.interval_alarm.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Header(
                    state = state,
                    onPlanEnabledChanged = { onAction(IntervalAlarmAction.OnPlanEnabledChanged(it)) },
                )
            }
            item {
                TimeRangeCard(
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
                    selectedInterval = state.intervalMinutes,
                    options = intervalOptions,
                    onIntervalSelected = { onAction(IntervalAlarmAction.OnIntervalMinutesChanged(it)) },
                )
            }
            item {
                DaysCard(
                    activeDays = state.activeDays,
                    onDayToggle = { onAction(IntervalAlarmAction.OnWeekdayToggle(it)) },
                )
            }
            item {
                PreviewCard(state = state)
            }
            item {
                Button(
                    onClick = { onAction(IntervalAlarmAction.OnSavePlanClick) },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save Plan")
                }
            }
            if (state.statusMessage != null) {
                item {
                    StatusMessage(
                        message = state.statusMessage,
                        onDismiss = { onAction(IntervalAlarmAction.OnDismissStatusMessage) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    state: IntervalAlarmState,
    onPlanEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Interval Alarm",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Wake window: ${state.startTime.toUiLabel()} - ${state.endTime.toUiLabel()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Plan enabled",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = state.isPlanEnabled,
                    onCheckedChange = onPlanEnabledChanged,
                )
            }
        }
    }
}

@Composable
private fun TimeRangeCard(
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Wake range",
                style = MaterialTheme.typography.titleLarge,
            )
            TimeAdjusterRow(
                title = "Start",
                time = startTime,
                onHourChanged = onStartHourChanged,
                onMinuteChanged = onStartMinuteChanged,
            )
            HorizontalDivider()
            TimeAdjusterRow(
                title = "End",
                time = endTime,
                onHourChanged = onEndHourChanged,
                onMinuteChanged = onEndMinuteChanged,
            )
        }
    }
}

@Composable
private fun TimeAdjusterRow(
    title: String,
    time: TimeOfDay,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberAdjuster(
                label = "Hour",
                value = time.hour,
                min = 0,
                max = 23,
                onValueChanged = onHourChanged,
            )
            NumberAdjuster(
                label = "Min",
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
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = { onValueChanged(cycle(value - step, min, max)) }) {
                Text("-")
            }
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { onValueChanged(cycle(value + step, min, max)) }) {
                Text("+")
            }
        }
    }
}

@Composable
private fun IntervalCard(
    selectedInterval: Int,
    options: List<Int>,
    onIntervalSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Interval",
                style = MaterialTheme.typography.titleLarge,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    FilterChip(
                        selected = selectedInterval == option,
                        onClick = { onIntervalSelected(option) },
                        label = { Text("$option min") },
                    )
                }
            }
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Repeat days",
                style = MaterialTheme.typography.titleLarge,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Weekday.entries.forEach { weekday ->
                    FilterChip(
                        selected = weekday in activeDays,
                        onClick = { onDayToggle(weekday) },
                        label = { Text(weekday.shortLabel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    state: IntervalAlarmState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Schedule preview",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "${state.alarmsPerActiveDay} alarms per active day",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${state.totalAlarmsPerWeek} total alarms per week",
                style = MaterialTheme.typography.bodyMedium,
            )
            HorizontalDivider()
            if (state.previewTimes.isEmpty()) {
                Text(
                    text = "No alarms in this range.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                val previewItems = state.previewTimes.take(PREVIEW_LIMIT)
                previewItems.forEach {
                    Text(
                        text = it.toUiLabel(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (state.previewTimes.size > PREVIEW_LIMIT) {
                    Text(
                        text = "+ ${state.previewTimes.size - PREVIEW_LIMIT} more",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Dismiss")
            }
        }
    }
}

private fun cycle(value: Int, min: Int, max: Int): Int {
    if (value < min) return max
    if (value > max) return min
    return value
}

private const val PREVIEW_LIMIT = 10
private val intervalOptions = listOf(2, 5, 10, 15)

@Preview
@Composable
private fun IntervalAlarmScreenPreview() {
    WakeAppTheme {
        IntervalAlarmScreen(
            state = IntervalAlarmState(
                previewTimes = listOf(
                    TimeOfDay(7, 0),
                    TimeOfDay(7, 5),
                    TimeOfDay(7, 10),
                    TimeOfDay(7, 15),
                ),
                alarmsPerActiveDay = 4,
                totalAlarmsPerWeek = 20,
            ),
            onAction = {},
        )
    }
}
