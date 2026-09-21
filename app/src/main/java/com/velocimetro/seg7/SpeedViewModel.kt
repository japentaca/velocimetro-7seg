package com.velocimetro.seg7

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

private const val MAX_HORIZONTAL_ACCURACY_METERS = 30f
private const val MAX_SPEED_ACCURACY_MPS = 2.5f
private const val MIN_MEAN_CN0 = 18
private const val MIN_SATELLITES = 5
private const val MIN_SPEED_KMH = 2f
private const val STALE_FIX_MILLIS = 5000L
private const val SMOOTHING = 0.45f

data class SpeedUiState(
    val speedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val tripMeters: Double = 0.0,
    val elapsedMs: Long = 0L,
    val accuracyMeters: Float? = null,
    val speedAccuracyMps: Float? = null,
    val hasFix: Boolean = false
)

class SpeedViewModel : ViewModel() {

    private val _state = MutableStateFlow(SpeedUiState())
    val state: StateFlow<SpeedUiState> = _state.asStateFlow()

    private var locationManager: LocationManager? = null
    private var listener: LocationListenerCompat? = null
    private var lastLocation: Location? = null
    private var smoothedKmh = 0f
    private var lastGoodFixMs = 0L
    private var started = false
    private var tripStartMs = 0L

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (started) return
        started = true
        tripStartMs = System.currentTimeMillis()

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = manager

        val provider = when {
            manager.allProviders.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.allProviders.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.GPS_PROVIDER
        }

        val request = LocationRequestCompat.Builder(1000L)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(0f)
            .build()

        val locationListener = LocationListenerCompat { location -> onLocation(location) }
        listener = locationListener

        try {
            LocationManagerCompat.requestLocationUpdates(
                manager,
                provider,
                request,
                locationListener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.w("Velocimetro", "No se pudo registrar el listener de ubicación", e)
            _state.update { it.copy(hasFix = false) }
        }

        viewModelScope.launch {
            while (true) {
                delay(500L)
                val now = System.currentTimeMillis()
                _state.update { it.copy(elapsedMs = (now - tripStartMs).coerceAtLeast(0L)) }
            }
        }
    }

    private fun onLocation(location: Location) {
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        val speedAccuracy =
            if (location.hasSpeedAccuracy()) location.speedAccuracyMetersPerSecond else null
        val satellites = location.extras?.getInt("satellites", -1) ?: -1
        val meanCn0 = location.extras?.getInt("meanCn0", -1) ?: -1

        val horizontalOk = accuracy != null && accuracy <= MAX_HORIZONTAL_ACCURACY_METERS
        val speedQualityOk = speedAccuracy == null || speedAccuracy <= MAX_SPEED_ACCURACY_MPS
        val signalOk = (satellites < 0 || satellites >= MIN_SATELLITES) &&
            (meanCn0 < 0 || meanCn0 >= MIN_MEAN_CN0)

        val trusted = location.hasSpeed() && horizontalOk && speedQualityOk && signalOk

        val now = System.currentTimeMillis()

        if (trusted) {
            lastGoodFixMs = now
            var instant = location.speed * 3.6f
            if (!instant.isFinite() || instant < 0f) instant = 0f
            if (instant < MIN_SPEED_KMH) instant = 0f
            smoothedKmh = if (smoothedKmh <= 0f) {
                instant
            } else {
                smoothedKmh * (1f - SMOOTHING) + instant * SMOOTHING
            }
            if (smoothedKmh < 1f) smoothedKmh = 0f
        } else {
            val sinceLastGoodFix = if (lastGoodFixMs == 0L) Long.MAX_VALUE else now - lastGoodFixMs
            if (sinceLastGoodFix > STALE_FIX_MILLIS) {
                smoothedKmh *= 0.5f
                if (smoothedKmh < 1f) smoothedKmh = 0f
            }
        }

        val previous = lastLocation
        var addedMeters = 0.0
        if (previous != null && trusted) {
            val dt = deltaSeconds(previous, location).coerceIn(0.0, 5.0)
            if (dt > 0.0) {
                val delta = (smoothedKmh / 3.6f).toDouble() * dt
                if (delta.isFinite() && delta in 0.0..400.0) addedMeters = delta
            }
        }
        lastLocation = location

        _state.update { current ->
            val trip = current.tripMeters + addedMeters
            val newMax = if (trusted) max(current.maxSpeedKmh, smoothedKmh) else current.maxSpeedKmh
            current.copy(
                speedKmh = smoothedKmh,
                maxSpeedKmh = newMax,
                tripMeters = trip,
                accuracyMeters = accuracy,
                speedAccuracyMps = speedAccuracy,
                hasFix = trusted
            )
        }
    }

    private fun deltaSeconds(previous: Location, current: Location): Double {
        val previousNanos = previous.elapsedRealtimeNanos
        val currentNanos = current.elapsedRealtimeNanos
        return if (previousNanos > 0L && currentNanos > previousNanos) {
            (currentNanos - previousNanos) / 1_000_000_000.0
        } else {
            (current.time - previous.time) / 1000.0
        }
    }

    fun resetTrip() {
        tripStartMs = System.currentTimeMillis()
        lastLocation = null
        _state.update { it.copy(tripMeters = 0.0, elapsedMs = 0L) }
    }

    fun resetMax() {
        _state.update { it.copy(maxSpeedKmh = 0f) }
    }

    override fun onCleared() {
        super.onCleared()
        val manager = locationManager
        val locationListener = listener
        if (manager != null && locationListener != null) {
            manager.removeUpdates(locationListener)
        }
    }
}
