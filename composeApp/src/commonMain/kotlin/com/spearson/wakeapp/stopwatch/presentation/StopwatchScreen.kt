package com.spearson.wakeapp.stopwatch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StopwatchScreen(
    state: StopwatchState,
    onAction: (StopwatchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isInitial = !state.isRunning && state.elapsedMillis == 0L
    val isPaused = !state.isRunning && state.elapsedMillis > 0L
    val lapsDescending = state.laps.asReversed()

    val hasLapHighlights = state.laps.size >= 2
    val fastestLapDuration = if (hasLapHighlights) state.laps.minOf { it.durationMillis } else null
    val slowestLapDuration = if (hasLapHighlights) state.laps.maxOf { it.durationMillis } else null
    val shouldHighlight = hasLapHighlights && fastestLapDuration != slowestLapDuration

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StopwatchBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Stopwatch",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.elapsedMillis.toStopwatchLabel(),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Light),
                color = StopwatchPrimary,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
            )

            StopwatchButtons(
                isInitial = isInitial,
                isPaused = isPaused,
                onStartClick = { onAction(StopwatchAction.OnStartClick) },
                onPauseClick = { onAction(StopwatchAction.OnPauseClick) },
                onContinueClick = { onAction(StopwatchAction.OnContinueClick) },
                onStopClick = { onAction(StopwatchAction.OnStopClick) },
                onLapClick = { onAction(StopwatchAction.OnLapClick) },
            )

            HorizontalDivider(color = StopwatchDivider)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(lapsDescending, key = { it.lapNumber }) { lap ->
                    val textColor = when {
                        shouldHighlight && lap.durationMillis == fastestLapDuration -> StopwatchFastest
                        shouldHighlight && lap.durationMillis == slowestLapDuration -> StopwatchSlowest
                        else -> Color.White
                    }
                    StopwatchLapRow(
                        lap = lap,
                        textColor = textColor,
                    )
                    HorizontalDivider(color = StopwatchDivider)
                }
            }
        }
    }
}

@Composable
private fun StopwatchButtons(
    isInitial: Boolean,
    isPaused: Boolean,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onContinueClick: () -> Unit,
    onStopClick: () -> Unit,
    onLapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            isInitial -> {
                Button(
                    onClick = onLapClick,
                    enabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = StopwatchButtonNeutral,
                        disabledContentColor = Color.White.copy(alpha = 0.55f),
                    ),
                ) {
                    Text("Lap")
                }
                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StopwatchPrimary,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Start")
                }
            }

            isPaused -> {
                Button(
                    onClick = onStopClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StopwatchDangerDark,
                        contentColor = StopwatchSlowest,
                    ),
                ) {
                    Text("Stop")
                }
                Button(
                    onClick = onContinueClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StopwatchPrimary,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Continue")
                }
            }

            else -> {
                Button(
                    onClick = onLapClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StopwatchButtonNeutral,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Lap")
                }
                Button(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StopwatchDangerDark,
                        contentColor = StopwatchSlowest,
                    ),
                ) {
                    Text("Pause")
                }
            }
        }
    }
}

@Composable
private fun StopwatchLapRow(
    lap: StopwatchLap,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Lap ${lap.lapNumber}",
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
        )
        Text(
            text = lap.durationMillis.toStopwatchLabel(),
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
        )
    }
}

private fun Long.toStopwatchLabel(): String {
    val safeMillis = coerceAtLeast(0L)
    val centiseconds = (safeMillis / 10L).toInt()
    val cs = centiseconds % 100
    val totalSeconds = centiseconds / 100
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60

    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${
            cs.toString().padStart(2, '0')
        }"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${cs.toString().padStart(2, '0')}"
    }
}

private val StopwatchBackground = Color(0xFF0A0A0A)
private val StopwatchPrimary = Color(0xFF3B82F6)
private val StopwatchButtonNeutral = Color(0xFF1B1B1E)
private val StopwatchDangerDark = Color(0xFF2A0F14)
private val StopwatchSlowest = Color(0xFFEF4444)
private val StopwatchFastest = Color(0xFF22C55E)
private val StopwatchDivider = Color(0xFF1A1A1A)
