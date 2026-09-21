package com.velocimetro.seg7

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
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

data class SpeedUiState(
    val speedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val tripMeters: Double = 0.0,
    val elapsedMs: Long = 0L,
    val accuracyMeters: Float? = null,
    val hasFix: Boolean = false
)

class SpeedViewModel : ViewModel() {

    private val _state = MutableStateFlow(SpeedUiState())
    val state: StateFlow<SpeedUiState> = _state.asStateFlow()

    private var locationManager: LocationManager? = null
    private var listener: LocationListenerCompat? = null
    private var lastLocation: Location? = null
    private var smoothedKmh = 0f
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
        val usable = accuracy != null && accuracy <= 40f

        var instant = if (location.hasSpeed()) location.speed * 3.6f else 0f
        if (!instant.isFinite() || instant < 0f) instant = 0f
        if (instant < 2f) instant = 0f

        smoothedKmh = if (smoothedKmh <= 0f) {
            instant
        } else {
            smoothedKmh * 0.55f + instant * 0.45f
        }
        if (smoothedKmh < 1f) smoothedKmh = 0f

        val previous = lastLocation
        var addedMeters = 0.0
        if (previous != null && usable) {
            val dt = deltaSeconds(previous, location).coerceIn(0.0, 5.0)
            if (dt > 0.0) {
                val delta = if (location.hasSpeed()) {
                    (smoothedKmh / 3.6f).toDouble() * dt
                } else {
                    previous.distanceTo(location).toDouble()
                }
                if (delta.isFinite() && delta in 0.0..400.0) addedMeters = delta
            }
        }
        lastLocation = location

        _state.update { current ->
            val trip = current.tripMeters + addedMeters
            val newMax = if (usable) max(current.maxSpeedKmh, smoothedKmh) else current.maxSpeedKmh
            current.copy(
                speedKmh = smoothedKmh,
                maxSpeedKmh = newMax,
                tripMeters = trip,
                accuracyMeters = accuracy,
                hasFix = usable
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
