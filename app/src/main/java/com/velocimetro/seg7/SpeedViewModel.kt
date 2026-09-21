package com.velocimetro.seg7

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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

    private var client: FusedLocationProviderClient? = null
    private var callback: LocationCallback? = null
    private var lastLocation: Location? = null
    private var smoothedKmh = 0f
    private var started = false
    private var tripStartMs = 0L

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (started) return
        started = true
        tripStartMs = System.currentTimeMillis()

        val fused = LocationServices.getFusedLocationProviderClient(context)
        client = fused

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onLocation(it) }
            }
        }
        callback = cb
        fused.requestLocationUpdates(request, cb, Looper.getMainLooper())

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
            val dt = ((location.time - previous.time) / 1000.0).coerceIn(0.0, 5.0)
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
        val cb = callback
        val fused = client
        if (cb != null && fused != null) fused.removeLocationUpdates(cb)
    }
}
