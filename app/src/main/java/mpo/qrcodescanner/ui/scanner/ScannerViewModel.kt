package mpo.qrcodescanner.ui.scanner

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpo.qrcodescanner.data.AppDatabase
import mpo.qrcodescanner.data.ScanRepository
import mpo.qrcodescanner.data.ScanResult

data class ScannerState(
    val lastScannedCode: ScannedCode? = null,
    val error: String? = null,
    val isFlashlightOn: Boolean = false,
    val isSoundEnabled: Boolean = true
)

data class ScannedCode(
    val content: String,
    val type: String
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScanRepository
    private val _state = MutableStateFlow(ScannerState())
    val state = _state.asStateFlow()
    private var toneGenerator: ToneGenerator? = null

    var isFlashlightOn by mutableStateOf(false)
        private set

    init {
        val dao = AppDatabase.getDatabase(application).scanResultDao()
        repository = ScanRepository(dao)
        initializeToneGenerator()
    }

    private fun initializeToneGenerator() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            // Handle initialization failure silently
        }
    }

    fun onQRCodeDetected(content: String, type: String) {
        viewModelScope.launch {
            try {
                // Save to database
                repository.insert(ScanResult(content = content, type = type))
                
                // Update UI state
                _state.value = _state.value.copy(
                    lastScannedCode = ScannedCode(content, type),
                    error = null
                )
                
                // Provide feedback
                provideHapticFeedback()
                playBeepSound()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to save scan: ${e.message}"
                )
            }
        }
    }

    private fun playBeepSound() {
        if (_state.value.isSoundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } catch (e: Exception) {
                // Handle any potential tone generator errors silently
            }
        }
    }

    fun toggleSound() {
        _state.value = _state.value.copy(
            isSoundEnabled = !_state.value.isSoundEnabled
        )
    }

    fun clearLastScannedCode() {
        _state.value = _state.value.copy(
            lastScannedCode = null,
            error = null
        )
    }

    fun toggleFlashlight() {
        isFlashlightOn = !isFlashlightOn
        _state.value = _state.value.copy(isFlashlightOn = isFlashlightOn)
    }

    private fun provideHapticFeedback() {
        val context = getApplication<Application>()
        
        // Vibration feedback
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
        toneGenerator = null
    }
} 