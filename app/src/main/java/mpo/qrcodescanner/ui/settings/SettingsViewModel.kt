package mpo.qrcodescanner.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

data class SettingsState(
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Float = 0.5f,
    val soundEnabled: Boolean = true,
    val selectedSound: String = "Default",
    val autoSaveEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val scannerBrightness: Float = 0.7f
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(loadSettings())
    val settingsState: StateFlow<SettingsState> = _settingsState

    private fun loadSettings(): SettingsState {
        return SettingsState(
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            vibrationIntensity = prefs.getFloat("vibration_intensity", 0.5f),
            soundEnabled = prefs.getBoolean("sound_enabled", true),
            selectedSound = prefs.getString("selected_sound", "Default") ?: "Default",
            autoSaveEnabled = prefs.getBoolean("auto_save_enabled", true),
            themeMode = ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name),
            scannerBrightness = prefs.getFloat("scanner_brightness", 0.7f)
        )
    }

    private fun saveSettings(settings: SettingsState) {
        viewModelScope.launch {
            prefs.edit().apply {
                putBoolean("vibration_enabled", settings.vibrationEnabled)
                putFloat("vibration_intensity", settings.vibrationIntensity)
                putBoolean("sound_enabled", settings.soundEnabled)
                putString("selected_sound", settings.selectedSound)
                putBoolean("auto_save_enabled", settings.autoSaveEnabled)
                putString("theme_mode", settings.themeMode.name)
                putFloat("scanner_brightness", settings.scannerBrightness)
                apply()
            }
        }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        _settingsState.value = _settingsState.value.copy(vibrationEnabled = enabled)
        saveSettings(_settingsState.value)
    }

    fun updateVibrationIntensity(intensity: Float) {
        _settingsState.value = _settingsState.value.copy(vibrationIntensity = intensity)
        saveSettings(_settingsState.value)
    }

    fun updateSoundEnabled(enabled: Boolean) {
        _settingsState.value = _settingsState.value.copy(soundEnabled = enabled)
        saveSettings(_settingsState.value)
    }

    fun updateSelectedSound(sound: String) {
        _settingsState.value = _settingsState.value.copy(selectedSound = sound)
        saveSettings(_settingsState.value)
    }

    fun updateAutoSaveEnabled(enabled: Boolean) {
        _settingsState.value = _settingsState.value.copy(autoSaveEnabled = enabled)
        saveSettings(_settingsState.value)
    }

    fun updateThemeMode(mode: ThemeMode) {
        _settingsState.value = _settingsState.value.copy(themeMode = mode)
        saveSettings(_settingsState.value)
    }

    fun updateScannerBrightness(brightness: Float) {
        _settingsState.value = _settingsState.value.copy(scannerBrightness = brightness)
        saveSettings(_settingsState.value)
    }

    fun sendSupportEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:zulfiqar@mypaperlessoffice.org")
            putExtra(Intent.EXTRA_SUBJECT, "QR Scanner Support")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
} 