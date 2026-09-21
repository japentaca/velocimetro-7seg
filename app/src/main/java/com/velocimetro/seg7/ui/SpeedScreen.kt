package com.velocimetro.seg7.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velocimetro.seg7.HudSettings
import com.velocimetro.seg7.SettingsStore
import com.velocimetro.seg7.SpeedUiState
import com.velocimetro.seg7.SpeedViewModel
import java.util.Locale
import kotlin.math.roundToInt

private const val TAG = "Velocimetro"

private val Warning = Color(0xFFFFC107)
private val Bad = Color(0xFFFF5252)

@Composable
fun SpeedScreen(vm: SpeedViewModel) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val store = remember { SettingsStore(context) }
    var settings by remember { mutableStateOf(store.load()) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.start(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = if (settings.mirrorHorizontal) -1f else 1f
                    rotationZ = if (settings.rotate180) 180f else 0f
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                if (!settings.hudMode) {
                    GpsBadge(state)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Seg7Display(
                        text = formatSpeed(state.speedKmh),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (!settings.hudMode) {
                    StatsRow(state)
                }
            }
        }

        IconButton(
            onClick = {
                Log.d(TAG, "Abrir ajustes")
                showSettings = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(4.dp)
                .size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Ajustes",
                tint = LedGreen.copy(alpha = 0.55f)
            )
        }

        if (showSettings) {
            SettingsPanel(
                settings = settings,
                onSettingsChange = {
                    settings = it
                    store.save(it)
                },
                onResetTrip = vm::resetTrip,
                onResetMax = vm::resetMax,
                onDismiss = { showSettings = false }
            )
        }
    }
}

private fun formatSpeed(kmh: Float): String {
    val value = kmh.roundToInt().coerceIn(0, 999)
    return when {
        value < 10 -> "  $value"
        value < 100 -> " $value"
        else -> "$value"
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Composable
private fun GpsBadge(state: SpeedUiState) {
    val accuracy = state.accuracyMeters
    val color = when {
        !state.hasFix -> Bad
        accuracy == null -> Bad
        accuracy <= 12f -> LedGreen
        accuracy <= 30f -> Warning
        else -> Bad
    }
    val label = if (accuracy == null) {
        "SIN SEÑAL GPS"
    } else {
        "GPS ±${accuracy.roundToInt()} m"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = color.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun StatsRow(state: SpeedUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Stat("MÁX", "${state.maxSpeedKmh.roundToInt()} km/h")
        Stat("VIAJE", String.format(Locale.US, "%.2f km", state.tripMeters / 1000.0))
        Stat("TIEMPO", formatDuration(state.elapsedMs))
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = LedGreen.copy(alpha = 0.45f),
            fontSize = 11.sp,
            letterSpacing = 1.5.sp
        )
        Text(
            text = value,
            color = LedGreen.copy(alpha = 0.92f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsPanel(
    settings: HudSettings,
    onSettingsChange: (HudSettings) -> Unit,
    onResetTrip: () -> Unit,
    onResetMax: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onDismiss)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .heightIn(max = 420.dp)
                .clickable(enabled = false) {},
            colors = CardDefaults.cardColors(containerColor = Color(0xFF08120C))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Ajustes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LedGreen
                )
                Spacer(Modifier.size(8.dp))

                ToggleRow(
                    title = "Espejo horizontal",
                    subtitle = "Invierte la imagen de izquierda a derecha",
                    checked = settings.mirrorHorizontal
                ) { onSettingsChange(settings.copy(mirrorHorizontal = it)) }

                ToggleRow(
                    title = "Girar 180°",
                    subtitle = "Rota la imagen para el reflejo en el parabrisas",
                    checked = settings.rotate180
                ) { onSettingsChange(settings.copy(rotate180 = it)) }

                ToggleRow(
                    title = "Modo HUD",
                    subtitle = "Muestra solo los dígitos, sin datos extra",
                    checked = settings.hudMode
                ) { onSettingsChange(settings.copy(hudMode = it)) }

                Spacer(Modifier.size(12.dp))
                HorizontalDivider(color = LedGreen.copy(alpha = 0.15f))
                Spacer(Modifier.size(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onResetTrip) {
                        Text("Reiniciar viaje", color = LedGreen)
                    }
                    OutlinedButton(onClick = onResetMax) {
                        Text("Reiniciar máximo", color = LedGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = LedGreen)
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = LedGreen.copy(alpha = 0.5f)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
