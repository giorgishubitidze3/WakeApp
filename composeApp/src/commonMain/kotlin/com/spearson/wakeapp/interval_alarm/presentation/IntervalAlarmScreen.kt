package com.spearson.wakeapp.interval_alarm.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.spearson.wakeapp.core.theme.WakeAppTheme
import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import kotlin.math.abs
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun IntervalAlarmScreen(
    state: IntervalAlarmState,
    onAction: (IntervalAlarmAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusedTime = when (state.focusedWindow) {
        FocusedWindow.Start -> state.startTime
        FocusedWindow.End -> state.endTime
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IntervalEditorBackground),
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
                TimeSelectionCard(
                    startTime = state.startTime,
                    endTime = state.endTime,
                    focusedWindow = state.focusedWindow,
                    focusedHour12 = focusedTime.toHour12(),
                    focusedMinute = focusedTime.minute,
                    focusedIsAm = focusedTime.hour < 12,
                    activeDays = state.activeDays,
                    onFocusedWindowChanged = {
                        onAction(IntervalAlarmAction.OnFocusedWindowChanged(it))
                    },
                    onHourChanged = {
                        onAction(IntervalAlarmAction.OnHourChanged(it))
                    },
                    onMinuteChanged = {
                        onAction(IntervalAlarmAction.OnMinuteChanged(it))
                    },
                    onMeridiemChanged = {
                        onAction(IntervalAlarmAction.OnMeridiemChanged(it))
                    },
                    onWeekdayToggle = {
                        onAction(IntervalAlarmAction.OnWeekdayToggle(it))
                    },
                )
            }
            item {
                IntervalInputCard(
                    intervalInput = state.intervalInput,
                    onIntervalInputChanged = {
                        onAction(IntervalAlarmAction.OnIntervalInputChanged(it))
                    },
                    onDecreaseClick = {
                        onAction(IntervalAlarmAction.OnIntervalDecreaseClick)
                    },
                    onIncreaseClick = {
                        onAction(IntervalAlarmAction.OnIntervalIncreaseClick)
                    },
                )
            }
            item {
                ToggleCard(
                    title = "Randomize Interval",
                    subtitle = "Fluctuates for lighter wake-up",
                    checked = state.randomizeInterval,
                    onCheckedChange = {
                        onAction(IntervalAlarmAction.OnRandomizeIntervalChanged(it))
                    },
                )
            }
            item {
                SettingCard(
                    title = "Alarm Sound",
                    subtitle = "Morning Breeze",
                )
            }
            item {
                SettingCard(
                    title = "Haptics",
                    subtitle = "Gentle Pulse",
                )
            }
            if (state.statusMessage != null) {
                item {
                    Text(
                        text = state.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
        SaveAlarmFooter(
            isSaving = state.isSaving,
            onSaveClick = { onAction(IntervalAlarmAction.OnSavePlanClick) },
        )
    }
}

@Composable
private fun SaveAlarmFooter(
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = SaveFooterTopBorder,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .background(IntervalEditorBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Button(
            onClick = onSaveClick,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false,
                ),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = if (isSaving) "Saving..." else "Save Alarm",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
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
        TextButton(onClick = onCancelClick) {
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            text = "Set Smart Alarm",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.width(76.dp))
    }
}

@Composable
private fun TimeSelectionCard(
    startTime: TimeOfDay,
    endTime: TimeOfDay,
    focusedWindow: FocusedWindow,
    focusedHour12: Int,
    focusedMinute: Int,
    focusedIsAm: Boolean,
    activeDays: Set<Weekday>,
    onFocusedWindowChanged: (FocusedWindow) -> Unit,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    onMeridiemChanged: (Boolean) -> Unit,
    onWeekdayToggle: (Weekday) -> Unit,
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
                TimeWindowLabel(
                    title = "START WINDOW",
                    time = startTime.toHourMinuteLabel(),
                    isFocused = focusedWindow == FocusedWindow.Start,
                    onClick = { onFocusedWindowChanged(FocusedWindow.Start) },
                )
                Text(
                    text = ">",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TimeWindowLabel(
                    title = "END WINDOW",
                    time = endTime.toHourMinuteLabel(),
                    isFocused = focusedWindow == FocusedWindow.End,
                    onClick = { onFocusedWindowChanged(FocusedWindow.End) },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            TimeWheel(
                hour12 = focusedHour12,
                minute = focusedMinute,
                isAm = focusedIsAm,
                onHourSelected = onHourChanged,
                onMinuteSelected = onMinuteChanged,
                onMeridiemSelected = onMeridiemChanged,
            )

            ActiveDaysRow(
                activeDays = activeDays,
                onWeekdayToggle = onWeekdayToggle,
            )
        }
    }
}

@Composable
private fun TimeWindowLabel(
    title: String,
    time: String,
    isFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = time,
            style = MaterialTheme.typography.headlineMedium,
            color = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun TimeWheel(
    hour12: Int,
    minute: Int,
    isAm: Boolean,
    onHourSelected: (Int) -> Unit,
    onMinuteSelected: (Int) -> Unit,
    onMeridiemSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f))
            .height(WHEEL_VISIBLE_HEIGHT + (WHEEL_OUTER_VERTICAL_PADDING * 2))
            .padding(
                horizontal = WHEEL_OUTER_HORIZONTAL_PADDING,
                vertical = WHEEL_OUTER_VERTICAL_PADDING,
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = WHEEL_SELECTION_OFFSET)
                .fillMaxWidth()
                .height(WHEEL_ITEM_HEIGHT)
                .clip(MaterialTheme.shapes.medium)
                .background(TimeWheelSelectionBackground),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WheelColumns(
                hour12 = hour12,
                minute = minute,
                onHourSelected = onHourSelected,
                onMinuteSelected = onMinuteSelected,
                modifier = Modifier.weight(1f),
            )
            MeridiemColumn(
                isAm = isAm,
                onMeridiemSelected = onMeridiemSelected,
                modifier = Modifier.width(58.dp),
            )
        }
    }
}

@Composable
private fun WheelColumns(
    hour12: Int,
    minute: Int,
    onHourSelected: (Int) -> Unit,
    onMinuteSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(WHEEL_VISIBLE_HEIGHT)
            .clip(MaterialTheme.shapes.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WheelNumberColumn(
                values = (1..12).toList(),
                selectedValue = hour12,
                formatter = { it.toString().padStart(2, '0') },
                onValueSelected = onHourSelected,
                modifier = Modifier.width(62.dp),
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            WheelNumberColumn(
                values = (0..59).toList(),
                selectedValue = minute,
                formatter = { it.toString().padStart(2, '0') },
                onValueSelected = onMinuteSelected,
                modifier = Modifier.width(62.dp),
            )
        }
    }
}

@Composable
private fun WheelNumberColumn(
    values: List<Int>,
    selectedValue: Int,
    formatter: (Int) -> String,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onValueSelectedState = rememberUpdatedState(onValueSelected)
    val valuesSize = values.size
    val wheelItemCount = remember(valuesSize) {
        valuesSize * WHEEL_REPEAT_CYCLES
    }
    val initialSelectedValueIndex = values.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
    val wheelCenterBaseIndex = remember(valuesSize) {
        ((wheelItemCount / 2) / valuesSize) * valuesSize
    }
    val initialIndex = wheelCenterBaseIndex + initialSelectedValueIndex
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeightPx = with(LocalDensity.current) { WHEEL_ITEM_HEIGHT.roundToPx() }
    var shouldEmitUserSelection by remember(valuesSize) { mutableStateOf(false) }

    LaunchedEffect(selectedValue, values) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        val selectedValueIndex = values.indexOf(selectedValue).takeIf { it >= 0 } ?: return@LaunchedEffect
        val currentIndex = listState.firstVisibleItemIndex
        val targetIndex = findNearestWheelIndex(
            currentIndex = currentIndex,
            selectedValueIndex = selectedValueIndex,
            valuesSize = valuesSize,
            wheelItemCount = wheelItemCount,
        )
        if (targetIndex != currentIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState, values) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    shouldEmitUserSelection = true
                    return@collect
                }
                if (!shouldEmitUserSelection) return@collect
                val offsetStep = if (listState.firstVisibleItemScrollOffset >= itemHeightPx / 2) 1 else 0
                val centerIndex = listState.firstVisibleItemIndex + offsetStep
                val updatedValue = values[centerIndex % valuesSize]
                onValueSelectedState.value(updatedValue)
                shouldEmitUserSelection = false
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .height(WHEEL_VISIBLE_HEIGHT),
        flingBehavior = snapFlingBehavior,
        contentPadding = PaddingValues(vertical = WHEEL_ITEM_HEIGHT * WHEEL_CENTER_OFFSET),
    ) {
        items(count = wheelItemCount) { index ->
            val value = values[index % valuesSize]
            val offsetStep = if (listState.firstVisibleItemScrollOffset >= itemHeightPx / 2) 1 else 0
            val centerIndex = listState.firstVisibleItemIndex + offsetStep
            val distanceFromCenter = abs(index - centerIndex)
            val isCenterItem = distanceFromCenter == 0
            Text(
                text = formatter(value),
                style = if (isCenterItem) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = if (isCenterItem) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = when (distanceFromCenter) {
                            1 -> 0.75f
                            2 -> 0.4f
                            else -> 0.2f
                        },
                    )
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WHEEL_ITEM_HEIGHT)
                    .padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun WheelTextColumn(
    values: List<String>,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onValueSelectedState = rememberUpdatedState(onValueSelected)
    val valuesSize = values.size
    val wheelItemCount = remember(valuesSize) {
        valuesSize * WHEEL_REPEAT_CYCLES
    }
    val initialSelectedValueIndex = values.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
    val wheelCenterBaseIndex = remember(valuesSize) {
        ((wheelItemCount / 2) / valuesSize) * valuesSize
    }
    val initialIndex = wheelCenterBaseIndex + initialSelectedValueIndex
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeightPx = with(LocalDensity.current) { WHEEL_ITEM_HEIGHT.roundToPx() }
    var shouldEmitUserSelection by remember(valuesSize) { mutableStateOf(false) }

    LaunchedEffect(selectedValue, values) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        val selectedValueIndex = values.indexOf(selectedValue).takeIf { it >= 0 } ?: return@LaunchedEffect
        val currentIndex = listState.firstVisibleItemIndex
        val targetIndex = findNearestWheelIndex(
            currentIndex = currentIndex,
            selectedValueIndex = selectedValueIndex,
            valuesSize = valuesSize,
            wheelItemCount = wheelItemCount,
        )
        if (targetIndex != currentIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState, values) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    shouldEmitUserSelection = true
                    return@collect
                }
                if (!shouldEmitUserSelection) return@collect
                val offsetStep = if (listState.firstVisibleItemScrollOffset >= itemHeightPx / 2) 1 else 0
                val centerIndex = listState.firstVisibleItemIndex + offsetStep
                val updatedValue = values[centerIndex % valuesSize]
                onValueSelectedState.value(updatedValue)
                shouldEmitUserSelection = false
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.height(WHEEL_VISIBLE_HEIGHT),
        flingBehavior = snapFlingBehavior,
        contentPadding = PaddingValues(vertical = WHEEL_ITEM_HEIGHT * WHEEL_CENTER_OFFSET),
    ) {
        items(count = wheelItemCount) { index ->
            val value = values[index % valuesSize]
            val offsetStep = if (listState.firstVisibleItemScrollOffset >= itemHeightPx / 2) 1 else 0
            val centerIndex = listState.firstVisibleItemIndex + offsetStep
            val distanceFromCenter = abs(index - centerIndex)
            val isCenterItem = distanceFromCenter == 0
            Text(
                text = value,
                style = if (isCenterItem) {
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = if (isCenterItem) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = when (distanceFromCenter) {
                            1 -> 0.7f
                            2 -> 0.35f
                            else -> 0.18f
                        },
                    )
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WHEEL_ITEM_HEIGHT)
                    .padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun MeridiemColumn(
    isAm: Boolean,
    onMeridiemSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    WheelTextColumn(
        values = listOf("AM", "PM"),
        selectedValue = if (isAm) "AM" else "PM",
        onValueSelected = { selected -> onMeridiemSelected(selected == "AM") },
        modifier = modifier,
    )
}

@Composable
private fun ActiveDaysRow(
    activeDays: Set<Weekday>,
    onWeekdayToggle: (Weekday) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Active Days",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Weekday.entries.forEach { weekday ->
                val isActive = weekday in activeDays
                DayChip(
                    label = weekday.shortLabel.take(1),
                    selected = isActive,
                    onClick = { onWeekdayToggle(weekday) },
                )
            }
        }
    }
}

@Composable
private fun DayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 34.dp)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun IntervalInputCard(
    intervalInput: String,
    onIntervalInputChanged: (String) -> Unit,
    onDecreaseClick: () -> Unit,
    onIncreaseClick: () -> Unit,
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Interval (minutes)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SquareStepperButton(
                    label = "-",
                    onClick = onDecreaseClick,
                )
                OutlinedTextField(
                    value = intervalInput,
                    onValueChange = onIntervalInputChanged,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.width(110.dp),
                )
                SquareStepperButton(
                    label = "+",
                    onClick = onIncreaseClick,
                )
            }
        }
    }
}

@Composable
private fun SquareStepperButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
        )
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
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = HomeStyleToggleThumb,
                    uncheckedThumbColor = HomeStyleToggleThumb,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = HomeStyleToggleOffTrack,
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun SettingCard(
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun TimeOfDay.toHourMinuteLabel(): String {
    val hour12 = toHour12().toString().padStart(2, '0')
    val minuteLabel = minute.toString().padStart(2, '0')
    return "$hour12:$minuteLabel"
}

private fun TimeOfDay.toHour12(): Int {
    return when (val converted = hour % 12) {
        0 -> 12
        else -> converted
    }
}

private fun findNearestWheelIndex(
    currentIndex: Int,
    selectedValueIndex: Int,
    valuesSize: Int,
    wheelItemCount: Int,
): Int {
    if (valuesSize <= 0 || wheelItemCount <= 0) return 0

    val currentCycle = currentIndex / valuesSize
    val candidates = listOf(
        (currentCycle - 1) * valuesSize + selectedValueIndex,
        currentCycle * valuesSize + selectedValueIndex,
        (currentCycle + 1) * valuesSize + selectedValueIndex,
    ).filter { it in 0 until wheelItemCount }

    if (candidates.isEmpty()) {
        return selectedValueIndex.coerceIn(0, wheelItemCount - 1)
    }

    return candidates.minBy { abs(it - currentIndex) }
}

private const val WHEEL_REPEAT_CYCLES = 200
private const val WHEEL_CENTER_OFFSET = 2
private val WHEEL_ITEM_HEIGHT = 42.dp
private val WHEEL_VISIBLE_HEIGHT = WHEEL_ITEM_HEIGHT * 5
private val WHEEL_OUTER_HORIZONTAL_PADDING = 10.dp
private val WHEEL_OUTER_VERTICAL_PADDING = 10.dp
private val WHEEL_SELECTION_OFFSET = (-3).dp

private val IntervalEditorBackground = Color(0xFF0F1729)
private val SaveFooterTopBorder = Color(0xFF1E293B)
private val TimeWheelSelectionBackground = Color(0xFF24344E)
private val HomeStyleToggleOffTrack = Color(0xFF1E293B)
private val HomeStyleToggleThumb = Color(0xFFFFFFFF)

@Preview
@Composable
private fun IntervalAlarmScreenPreview() {
    WakeAppTheme {
        IntervalAlarmScreen(
            state = IntervalAlarmState(),
            onAction = {},
        )
    }
}
