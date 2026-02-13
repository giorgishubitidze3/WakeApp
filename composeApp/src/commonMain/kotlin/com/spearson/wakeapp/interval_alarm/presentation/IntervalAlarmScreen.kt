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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import com.spearson.wakeapp.interval_alarm.domain.model.AlarmRingtoneOption
import com.spearson.wakeapp.interval_alarm.domain.model.HapticsPattern
import com.spearson.wakeapp.core.theme.WakeAppTheme
import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.abs
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import wakeapp.composeapp.generated.resources.Res
import wakeapp.composeapp.generated.resources.ic_alarm_sound
import wakeapp.composeapp.generated.resources.ic_haptics
import wakeapp.composeapp.generated.resources.ic_window_range_swap

@Composable
fun IntervalAlarmScreen(
    state: IntervalAlarmState,
    onAction: (IntervalAlarmAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.screenMode == IntervalAlarmScreenMode.SoundSelection) {
        AlarmSoundSelectionScreen(
            state = state,
            onBackClick = { onAction(IntervalAlarmAction.OnAlarmSoundBackClick) },
            onDoneClick = { onAction(IntervalAlarmAction.OnAlarmSoundDoneClick) },
            onRingtoneSelected = { ringtoneId ->
                onAction(IntervalAlarmAction.OnAlarmRingtoneSelected(ringtoneId))
            },
            onPreviewToggle = { ringtoneId ->
                onAction(IntervalAlarmAction.OnAlarmRingtonePreviewToggle(ringtoneId))
            },
            onVolumeSliderChanged = { sliderValue ->
                onAction(IntervalAlarmAction.OnAlarmVolumeSliderChanged(sliderValue))
            },
            modifier = modifier,
        )
        return
    }
    if (state.screenMode == IntervalAlarmScreenMode.HapticsSelection) {
        HapticsSelectionScreen(
            state = state,
            onBackClick = { onAction(IntervalAlarmAction.OnHapticsBackClick) },
            onSaveClick = { onAction(IntervalAlarmAction.OnHapticsSaveClick) },
            onPatternSelected = { pattern ->
                onAction(IntervalAlarmAction.OnHapticsPatternSelected(pattern))
            },
            onHapticsOnlyChanged = { enabled ->
                onAction(IntervalAlarmAction.OnHapticsOnlyChanged(enabled))
            },
            onEscalateChanged = { enabled ->
                onAction(IntervalAlarmAction.OnHapticsEscalateChanged(enabled))
            },
            modifier = modifier,
        )
        return
    }

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
                    useTransparentStyle = true,
                )
            }
            item {
                SettingCard(
                    title = "Alarm Sound",
                    subtitle = if (state.hapticsOnly) {
                        "Silent / Haptics Only"
                    } else {
                        state.selectedRingtoneName
                    },
                    iconRes = Res.drawable.ic_alarm_sound,
                    onClick = { onAction(IntervalAlarmAction.OnAlarmSoundClick) },
                    useTransparentStyle = true,
                )
            }
            item {
                SettingCard(
                    title = "Haptics Pattern",
                    subtitle = state.hapticsSummary(),
                    iconRes = Res.drawable.ic_haptics,
                    onClick = { onAction(IntervalAlarmAction.OnHapticsClick) },
                    useTransparentStyle = true,
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
    buttonLabel: String = "Save Alarm",
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
                text = if (isSaving) "Saving..." else buttonLabel,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun AlarmSoundSelectionScreen(
    state: IntervalAlarmState,
    onBackClick: () -> Unit,
    onDoneClick: () -> Unit,
    onRingtoneSelected: (String) -> Unit,
    onPreviewToggle: (String) -> Unit,
    onVolumeSliderChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderValue = (
        state.ringtoneVolumePercent.coerceIn(0, FREE_TIER_MAX_VOLUME_PERCENT).toFloat() /
            FREE_TIER_MAX_VOLUME_PERCENT.toFloat()
        ) * FREE_TIER_SLIDER_MAX_VALUE
    val effectiveVolumePercent = state.ringtoneVolumePercent.coerceAtMost(FREE_TIER_MAX_VOLUME_PERCENT)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IntervalEditorBackground),
    ) {
        AlarmSoundHeader(
            onBackClick = onBackClick,
        )
        LazyColumn(
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
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Searching ringtones",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Searching alarm ringtones...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(
                items = state.availableRingtones,
                key = { it.id },
            ) { ringtone ->
                AlarmRingtoneRow(
                    option = ringtone,
                    isSelected = ringtone.id == state.selectedRingtoneId,
                    isPreviewPlaying = ringtone.id == state.previewPlayingRingtoneId,
                    onSelect = { onRingtoneSelected(ringtone.id) },
                    onPreviewToggle = { onPreviewToggle(ringtone.id) },
                )
            }
            if (state.availableRingtones.isEmpty()) {
                item {
                    Text(
                        text = "No alarm tones available on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            if (state.statusMessage != null) {
                item {
                    Text(
                        text = state.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Volume",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = onVolumeSliderChanged,
                valueRange = 0f..FREE_TIER_SLIDER_MAX_VALUE,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    thumbColor = Color.White,
                ),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VolumeTag(
                    label = "${effectiveVolumePercent.coerceAtLeast(0)}%",
                )
                VolumeTag(
                    label = "200% volume coming soon",
                    isMuted = true,
                )
            }
            Button(
                onClick = onDoneClick,
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
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
private fun AlarmSoundHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
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
            text = "Alarm Sound",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.width(84.dp))
    }
}

@Composable
private fun AlarmRingtoneRow(
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
        MaterialTheme.colorScheme.surface
    }
    val rowBorder = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = rowBackground,
        ),
        border = BorderStroke(width = 1.dp, color = rowBorder),
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
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
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
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VolumeTag(
    label: String,
    isMuted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isMuted) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                },
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isMuted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun HapticsSelectionScreen(
    state: IntervalAlarmState,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onPatternSelected: (HapticsPattern) -> Unit,
    onHapticsOnlyChanged: (Boolean) -> Unit,
    onEscalateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IntervalEditorBackground),
    ) {
        HapticsHeader(
            onBackClick = onBackClick,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionLabel(text = "PATTERNS")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        HapticsPattern.entries.forEachIndexed { index, pattern ->
                            HapticsPatternRow(
                                pattern = pattern,
                                isSelected = pattern == state.selectedHapticsPattern,
                                onClick = { onPatternSelected(pattern) },
                            )
                            if (index < HapticsPattern.entries.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            }
                        }
                    }
                }
            }
            item {
                SectionLabel(text = "SETTINGS")
            }
            item {
                ToggleCard(
                    title = "Haptics Only",
                    subtitle = "Vibrate without any sound",
                    checked = state.hapticsOnly,
                    onCheckedChange = onHapticsOnlyChanged,
                )
            }
            item {
                ToggleCard(
                    title = "Escalate Over Time",
                    subtitle = "Increase intensity gradually",
                    checked = state.hapticsEscalateOverTime,
                    onCheckedChange = onEscalateChanged,
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
            isSaving = false,
            onSaveClick = onSaveClick,
            buttonLabel = "Save",
        )
    }
}

@Composable
private fun HapticsHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBackClick) {
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = "Haptics",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(76.dp))
    }
}

@Composable
private fun HapticsPatternRow(
    pattern: HapticsPattern,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pattern.displayLabel(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isSelected) "Selected" else "Not selected",
            tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 8.dp),
    )
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
            text = "Set Alarm",
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeWindowLabel(
                title = "START WINDOW",
                time = startTime.toHourMinuteLabel(),
                isFocused = focusedWindow == FocusedWindow.Start,
                onClick = { onFocusedWindowChanged(FocusedWindow.Start) },
                modifier = Modifier.width(132.dp),
            )
            Icon(
                painter = painterResource(Res.drawable.ic_window_range_swap),
                contentDescription = "Range separator",
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
            TimeWindowLabel(
                title = "END WINDOW",
                time = endTime.toHourMinuteLabel(),
                isFocused = focusedWindow == FocusedWindow.End,
                onClick = { onFocusedWindowChanged(FocusedWindow.End) },
                modifier = Modifier.width(132.dp),
            )
        }

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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                NonFocusedWindowLabelColor
            },
        )
        Text(
            text = time,
            style = MaterialTheme.typography.headlineMedium,
            color = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                NonFocusedWindowTimeColor
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
            .border(
                width = 1.dp,
                color = TimeWheelBorderColor,
                shape = MaterialTheme.shapes.large,
            )
            .height(WHEEL_VISIBLE_HEIGHT + (WHEEL_OUTER_VERTICAL_PADDING * 0.75f))
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
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(WHEEL_EDGE_FADE_HEIGHT)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            IntervalEditorBackground.copy(alpha = 0.95f),
                            IntervalEditorBackground.copy(alpha = 0.7f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(WHEEL_EDGE_FADE_HEIGHT)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            IntervalEditorBackground.copy(alpha = 0.7f),
                            IntervalEditorBackground.copy(alpha = 0.95f),
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = WHEEL_SELECTION_OFFSET),
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
                modifier = Modifier.width(52.dp),
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
    val density = LocalDensity.current
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
                val updatedValue = values[centerIndex % valuesSize]
                onValueSelectedState.value(updatedValue)
                shouldEmitUserSelection = false
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.height(WHEEL_VISIBLE_HEIGHT),
        flingBehavior = snapFlingBehavior,
        // Use the exact math to ensure the center item snaps to the exact middle
        contentPadding = PaddingValues(vertical = (WHEEL_VISIBLE_HEIGHT - WHEEL_ITEM_HEIGHT) / 2),
    ) {
        items(count = wheelItemCount) { index ->
            val value = values[index % valuesSize]
            val distanceFromCenter = abs(index - centerIndex)
            val isCenterItem = distanceFromCenter == 0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WHEEL_ITEM_HEIGHT), // Rigid slot height
                contentAlignment = Alignment.Center // Absolute vertical centering
            ) {
                Text(
                    text = formatter(value),
                    style = if (isCenterItem) {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize *
                                    WHEEL_CENTER_TEXT_SIZE_MULTIPLIER,
                            // Line height helps the font's internal bounding box stay centered
                            lineHeight = with(density) { WHEEL_ITEM_HEIGHT.toSp() }
                        )
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    color = if (isCenterItem) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = when (distanceFromCenter) {
                                1 -> 0.4f // Lowered alpha slightly for better contrast
                                else -> 0.15f
                            },
                        )
                    },
                    textAlign = TextAlign.Center
                    // REMOVED: .height(WHEEL_ITEM_HEIGHT) from the Text itself
                    // Allowing the Box to handle the alignment prevents baseline "clumping"
                )
            }
        }
    }
}

@Composable
private fun WheelTextColumn(
    values: List<String>,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    visibleHeight: Dp = WHEEL_VISIBLE_HEIGHT,
    modifier: Modifier = Modifier,
) {
    val onValueSelectedState = rememberUpdatedState(onValueSelected)
    val valuesSize = values.size
    if (valuesSize == 0) return
    val initialSelectedValueIndex = values.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialSelectedValueIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    var shouldEmitUserSelection by remember(valuesSize) { mutableStateOf(false) }
    val centerIndex by remember(listState, valuesSize) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                listState.firstVisibleItemIndex.coerceIn(0, valuesSize - 1)
            } else {
                val viewportCenter = layoutInfo.viewportStartOffset +
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                val index = visibleItems.minByOrNull { itemInfo ->
                    abs((itemInfo.offset + (itemInfo.size / 2)) - viewportCenter)
                }?.index ?: listState.firstVisibleItemIndex
                index.coerceIn(0, valuesSize - 1)
            }
        }
    }

    LaunchedEffect(selectedValue, values) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        val selectedValueIndex = values.indexOf(selectedValue).takeIf { it >= 0 } ?: return@LaunchedEffect
        if (selectedValueIndex != listState.firstVisibleItemIndex) {
            listState.scrollToItem(selectedValueIndex)
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
                val updatedValue = values[centerIndex]
                onValueSelectedState.value(updatedValue)
                shouldEmitUserSelection = false
            }
    }

    val verticalContentPadding = ((visibleHeight - WHEEL_ITEM_HEIGHT) / 2).coerceAtLeast(0.dp)

    LazyColumn(
        state = listState,
        modifier = modifier
            .height(visibleHeight),
        flingBehavior = snapFlingBehavior,
        contentPadding = PaddingValues(vertical = verticalContentPadding),
    ) {
        items(count = valuesSize) { index ->
            val value = values[index]
            val distanceFromCenter = abs(index - centerIndex)
            val isCenterItem = distanceFromCenter == 0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WHEEL_ITEM_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    style = if (isCenterItem) {
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize *
                                WHEEL_MERIDIEM_CENTER_TEXT_SIZE_MULTIPLIER,
                        )
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = if (isCenterItem) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = when (distanceFromCenter) {
                                1 -> 0.7f
                                else -> 0.2f
                            },
                        )
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
        visibleHeight = WHEEL_MERIDIEM_VISIBLE_HEIGHT,
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
        MaterialTheme.colorScheme.primary.copy(alpha = 1.0f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 44.dp)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
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
            containerColor = Color.Transparent,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
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
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TimeWheelBorderColor,
                        unfocusedBorderColor = TimeWheelBorderColor,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.width(144.dp),
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
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = TimeWheelBorderColor,
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
    useTransparentStyle: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (useTransparentStyle) Color.Transparent else MaterialTheme.colorScheme.surface,
        ),
        border = if (useTransparentStyle) {
            BorderStroke(width = 1.dp, color = TimeWheelBorderColor)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
    iconRes: DrawableResource? = null,
    onClick: (() -> Unit)? = null,
    useTransparentStyle: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { base ->
                if (onClick != null) {
                    base.clickable(onClick = onClick)
                } else {
                    base
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (useTransparentStyle) Color.Transparent else MaterialTheme.colorScheme.surface,
        ),
        border = if (useTransparentStyle) {
            BorderStroke(width = 1.dp, color = TimeWheelBorderColor)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (iconRes != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = "$title icon",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
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
            }
            Icon(
                painter = painterResource(Res.drawable.ic_window_range_swap),
                contentDescription = "Settings Card Arrow",
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
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

private fun HapticsPattern.displayLabel(): String {
    return when (this) {
        HapticsPattern.NoneOff -> "None / Off"
        HapticsPattern.GentlePulse -> "Gentle Pulse"
        HapticsPattern.Standard -> "Standard"
        HapticsPattern.StrongBuzz -> "Strong Buzz"
        HapticsPattern.Heartbeat -> "Heartbeat"
        HapticsPattern.RapidFire -> "Rapid Fire"
    }
}

private fun IntervalAlarmState.hapticsSummary(): String {
    if (selectedHapticsPattern == HapticsPattern.NoneOff) {
        return "None / Off"
    }
    val base = selectedHapticsPattern.displayLabel()
    return "$base • ACTIVE"
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
private val WHEEL_ITEM_HEIGHT = 40.dp
private val WHEEL_SELECTION_OFFSET = 4.dp
private val WHEEL_VISIBLE_HEIGHT = (WHEEL_ITEM_HEIGHT * 4) + (WHEEL_SELECTION_OFFSET * 2)
private val WHEEL_MERIDIEM_VISIBLE_HEIGHT = (WHEEL_ITEM_HEIGHT * 3) + (WHEEL_SELECTION_OFFSET * 2)
private val WHEEL_OUTER_HORIZONTAL_PADDING = 6.dp
private val WHEEL_OUTER_VERTICAL_PADDING = 2.dp
private val WHEEL_EDGE_FADE_HEIGHT = 28.dp
private const val WHEEL_CENTER_TEXT_SIZE_MULTIPLIER = 1.08f
private const val WHEEL_MERIDIEM_CENTER_TEXT_SIZE_MULTIPLIER = 1.0f

private val IntervalEditorBackground = Color(0xFF0F1729)
private val SaveFooterTopBorder = Color(0xFF1E293B)
private val TimeWheelSelectionBackground = Color(0xFF1E293B)
private val TimeWheelBorderColor = Color(0xFF334155)
private val NonFocusedWindowLabelColor = Color(0xFFCBD5E1)
private val NonFocusedWindowTimeColor = Color(0xFFE2E8F0)
private val HomeStyleToggleOffTrack = Color(0xFF1E293B)
private val HomeStyleToggleThumb = Color(0xFFFFFFFF)
private const val FREE_TIER_MAX_VOLUME_PERCENT = 100
private const val FREE_TIER_SLIDER_MAX_VALUE = 0.5f

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
