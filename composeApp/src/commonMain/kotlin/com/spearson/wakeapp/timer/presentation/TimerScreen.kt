package com.spearson.wakeapp.timer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spearson.wakeapp.interval_alarm.domain.model.AlarmRingtoneOption
import kotlin.math.abs

@Composable
fun TimerScreen(
    state: TimerState,
    onAction: (TimerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.screenMode) {
        TimerScreenMode.Main -> TimerMainScreen(
            state = state,
            onAction = onAction,
            modifier = modifier,
        )

        TimerScreenMode.SoundSelection -> TimerSoundSelectionScreen(
            state = state,
            onBackClick = { onAction(TimerAction.OnSoundBackClick) },
            onDoneClick = { onAction(TimerAction.OnSoundDoneClick) },
            onRingtoneSelected = { ringtoneId ->
                onAction(TimerAction.OnRingtoneSelected(ringtoneId))
            },
            onPreviewToggle = { ringtoneId ->
                onAction(TimerAction.OnRingtonePreviewToggle(ringtoneId))
            },
            onVolumeChanged = { sliderValue ->
                onAction(TimerAction.OnVolumeChanged(sliderValue))
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun TimerMainScreen(
    state: TimerState,
    onAction: (TimerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rightButtonLabel = when (state.runState) {
        TimerRunState.Idle -> "Start"
        TimerRunState.Running -> "Pause"
        TimerRunState.Paused -> "Continue"
        TimerRunState.Completed -> "Stop"
    }
    val leftButtonEnabled = state.runState != TimerRunState.Idle || state.selectedDurationMillis > 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TimerBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Timer",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (state.runState == TimerRunState.Idle) {
                DurationWheelPicker(
                    hour = state.selectedHours,
                    minute = state.selectedMinutes,
                    second = state.selectedSeconds,
                    onHourSelected = { onAction(TimerAction.OnHourChanged(it)) },
                    onMinuteSelected = { onAction(TimerAction.OnMinuteChanged(it)) },
                    onSecondSelected = { onAction(TimerAction.OnSecondChanged(it)) },
                )
            } else {
                CountdownDisplay(remainingMillis = state.displayMillis)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TimerUnitLabel(text = "HOURS")
                TimerUnitLabel(text = "MIN")
                TimerUnitLabel(text = "SEC")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Button(
                    onClick = { onAction(TimerAction.OnCancelClick) },
                    enabled = leftButtonEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TimerCancelButton,
                        contentColor = Color.White,
                        disabledContainerColor = TimerCancelButton.copy(alpha = 0.6f),
                        disabledContentColor = Color.White.copy(alpha = 0.45f),
                    ),
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }

                Button(
                    onClick = {
                        when (state.runState) {
                            TimerRunState.Idle -> onAction(TimerAction.OnStartClick)

                            TimerRunState.Running -> onAction(TimerAction.OnPauseClick)
                            TimerRunState.Paused -> onAction(TimerAction.OnContinueClick)
                            TimerRunState.Completed -> onAction(TimerAction.OnCancelClick)
                        }
                    },
                    enabled = state.runState != TimerRunState.Idle || state.selectedDurationMillis > 0L,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TimerPrimaryButton,
                        contentColor = Color.White,
                        disabledContainerColor = TimerPrimaryButton.copy(alpha = 0.55f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f),
                    ),
                ) {
                    Text(
                        text = rightButtonLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(TimerAction.OnSoundClick) },
                colors = CardDefaults.cardColors(containerColor = TimerCardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, TimerCardBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "When Timer Ends",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Text(
                            text = state.selectedRingtoneName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TimerMutedText,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Open timer sound",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "COMPLETED TIMERS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TimerMutedText,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.completedTimers, key = { it.durationMillis }) { timer ->
                        CompletedTimerChip(
                            durationMillis = timer.durationMillis,
                            onClick = {
                                onAction(TimerAction.OnCompletedTimerClick(timer.durationMillis))
                            },
                        )
                    }
                }
            }

            if (state.statusMessage != null) {
                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(TimerAction.OnStatusMessageShown) },
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TimerSoundSelectionScreen(
    state: TimerState,
    onBackClick: () -> Unit,
    onDoneClick: () -> Unit,
    onRingtoneSelected: (String) -> Unit,
    onPreviewToggle: (String) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TimerBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "Timer Sound",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(88.dp))
        }

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.isLoadingRingtones) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TimerCardBackground)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Searching timer sounds",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Searching alarm ringtones...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TimerMutedText,
                        )
                    }
                }
            }

            items(state.availableRingtones, key = { it.id }) { ringtone ->
                TimerRingtoneRow(
                    option = ringtone,
                    isSelected = ringtone.id == state.selectedRingtoneId,
                    isPreviewPlaying = ringtone.id == state.previewPlayingRingtoneId,
                    onSelect = { onRingtoneSelected(ringtone.id) },
                    onPreviewToggle = { onPreviewToggle(ringtone.id) },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TimerBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HorizontalDivider(color = TimerCardBorder)
            Text(
                text = "Volume",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Slider(
                value = state.ringtoneVolumePercent / 100f,
                onValueChange = onVolumeChanged,
                valueRange = 0f..1f,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = TimerCardBorder,
                    thumbColor = Color.White,
                ),
            )
            Text(
                text = "${state.ringtoneVolumePercent}%",
                style = MaterialTheme.typography.bodyMedium,
                color = TimerMutedText,
            )
            Button(
                onClick = onDoneClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TimerPrimaryButton,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
private fun TimerRingtoneRow(
    option: AlarmRingtoneOption,
    isSelected: Boolean,
    isPreviewPlaying: Boolean,
    onSelect: () -> Unit,
    onPreviewToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowBackground = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    } else {
        TimerCardBackground
    }
    val rowBorder = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        TimerCardBorder
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = rowBackground),
        border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = rowBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(
                    onClick = onPreviewToggle,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPreviewPlaying) "Stop preview" else "Play preview",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else TimerMutedText,
            )
        }
    }
}

@Composable
private fun DurationWheelPicker(
    hour: Int,
    minute: Int,
    second: Int,
    onHourSelected: (Int) -> Unit,
    onMinuteSelected: (Int) -> Unit,
    onSecondSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TIMER_WHEEL_HEIGHT)
            .clip(RoundedCornerShape(20.dp))
            .background(TimerWheelBackground)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = TIMER_WHEEL_SELECTION_OFFSET)
                .fillMaxWidth()
                .height(TIMER_WHEEL_ITEM_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .background(TimerWheelSelectionBackground),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DurationWheelColumn(
                values = (0..23).toList(),
                selectedValue = hour,
                onValueSelected = onHourSelected,
                modifier = Modifier.width(72.dp),
            )
            DurationWheelColumn(
                values = (0..59).toList(),
                selectedValue = minute,
                onValueSelected = onMinuteSelected,
                modifier = Modifier.width(72.dp),
            )
            DurationWheelColumn(
                values = (0..59).toList(),
                selectedValue = second,
                onValueSelected = onSecondSelected,
                modifier = Modifier.width(72.dp),
            )
        }
    }
}

@Composable
private fun DurationWheelColumn(
    values: List<Int>,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onValueSelectedState = rememberUpdatedState(onValueSelected)
    val valuesSize = values.size
    val wheelItemCount = remember(valuesSize) { valuesSize * TIMER_WHEEL_REPEAT_CYCLES }
    val selectedValueIndex = values.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
    val centerBaseIndex = remember(valuesSize, wheelItemCount) {
        ((wheelItemCount / 2) / valuesSize) * valuesSize
    }
    val initialIndex = centerBaseIndex + selectedValueIndex
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    var shouldEmitUserSelection by remember(valuesSize) { mutableStateOf(false) }
    val centerIndex by remember(listState, valuesSize) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                listState.firstVisibleItemIndex
            } else {
                val viewportCenter = layoutInfo.viewportStartOffset +
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                visibleItems.minByOrNull { itemInfo ->
                    abs((itemInfo.offset + (itemInfo.size / 2)) - viewportCenter)
                }?.index ?: listState.firstVisibleItemIndex
            }
        }
    }

    LaunchedEffect(selectedValue, values) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        val targetIndex = findNearestWheelIndex(
            currentIndex = listState.firstVisibleItemIndex,
            selectedValueIndex = selectedValueIndex,
            valuesSize = valuesSize,
            wheelItemCount = wheelItemCount,
        )
        if (targetIndex != listState.firstVisibleItemIndex) {
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
                val updatedValue = values[centerIndex % valuesSize]
                onValueSelectedState.value(updatedValue)
                shouldEmitUserSelection = false
            }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        modifier = modifier.height(TIMER_WHEEL_VISIBLE_HEIGHT),
        flingBehavior = snapFlingBehavior,
        contentPadding = PaddingValues(
            vertical = (TIMER_WHEEL_VISIBLE_HEIGHT - TIMER_WHEEL_ITEM_HEIGHT) / 2
        ),
    ) {
        items(count = wheelItemCount) { index ->
            val value = values[index % valuesSize]
            val distanceFromCenter = abs(index - centerIndex)
            val isCenterItem = distanceFromCenter == 0
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TIMER_WHEEL_ITEM_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value.toString().padStart(2, '0'),
                    style = if (isCenterItem) {
                        MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    color = if (isCenterItem) {
                        Color.White
                    } else {
                        TimerWheelFaded.copy(
                            alpha = when (distanceFromCenter) {
                                1 -> 0.7f
                                2 -> 0.38f
                                else -> 0.2f
                            },
                        )
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CountdownDisplay(
    remainingMillis: Long,
    modifier: Modifier = Modifier,
) {
    val totalSeconds = (remainingMillis.coerceAtLeast(0L) / TimerState.MILLIS_PER_SECOND).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TimerWheelBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${hours.toString().padStart(2, '0')} : ${minutes.toString().padStart(2, '0')} : ${seconds.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimerUnitLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = TimerMutedText,
        modifier = modifier,
    )
}

@Composable
private fun CompletedTimerChip(
    durationMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(40.dp))
            .background(TimerChipBackground)
            .border(
                width = 1.dp,
                color = TimerCardBorder,
                shape = RoundedCornerShape(40.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = durationMillis.toDurationLabel(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Long.toDurationLabel(): String {
    val totalSeconds = (coerceAtLeast(0L) / TimerState.MILLIS_PER_SECOND).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

private fun findNearestWheelIndex(
    currentIndex: Int,
    selectedValueIndex: Int,
    valuesSize: Int,
    wheelItemCount: Int,
): Int {
    val currentModulo = currentIndex % valuesSize
    val offset = selectedValueIndex - currentModulo
    val candidate = currentIndex + offset
    return candidate.coerceIn(0, wheelItemCount - 1)
}

private val TimerBackground = Color(0xFF0A0A0A)
private val TimerWheelBackground = Color(0xFF050B14)
private val TimerWheelSelectionBackground = Color(0xFF0C1930)
private val TimerWheelFaded = Color(0xFF7E8BA0)
private val TimerMutedText = Color(0xFF93A0B5)
private val TimerCancelButton = Color(0xFF1E293B)
private val TimerPrimaryButton = Color(0xFF1E5CD7)
private val TimerCardBackground = Color(0xFF081225)
private val TimerCardBorder = Color(0xFF1E293B)
private val TimerChipBackground = Color(0xFF111E35)

private val TIMER_WHEEL_HEIGHT = 170.dp
private val TIMER_WHEEL_VISIBLE_HEIGHT = 146.dp
private val TIMER_WHEEL_ITEM_HEIGHT = 40.dp
private val TIMER_WHEEL_SELECTION_OFFSET = 0.dp
private const val TIMER_WHEEL_REPEAT_CYCLES = 100
