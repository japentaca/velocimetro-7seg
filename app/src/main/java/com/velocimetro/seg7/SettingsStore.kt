package com.velocimetro.seg7

import android.content.Context

data class HudSettings(
    val mirrorHorizontal: Boolean = false,
    val rotate180: Boolean = false,
    val hudMode: Boolean = false
)

class SettingsStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("velocimetro", Context.MODE_PRIVATE)

    fun load(): HudSettings = HudSettings(
        mirrorHorizontal = prefs.getBoolean(KEY_MIRROR, false),
        rotate180 = prefs.getBoolean(KEY_ROTATE, false),
        hudMode = prefs.getBoolean(KEY_HUD, false)
    )

    fun save(settings: HudSettings) {
        prefs.edit()
            .putBoolean(KEY_MIRROR, settings.mirrorHorizontal)
            .putBoolean(KEY_ROTATE, settings.rotate180)
            .putBoolean(KEY_HUD, settings.hudMode)
            .apply()
    }

    private companion object {
        const val KEY_MIRROR = "mirror_horizontal"
        const val KEY_ROTATE = "rotate_180"
        const val KEY_HUD = "hud_mode"
    }
}
