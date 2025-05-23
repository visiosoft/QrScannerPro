package mpo.qrcodescanner.ui.scanner

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpo.qrcodescanner.data.QRCodeScan
import mpo.qrcodescanner.data.QRCodeDatabase
import java.util.Date

data class ScannerState(
    val lastScannedCode: QRCodeScan? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFlashlightOn: Boolean = false
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = QRCodeDatabase.getDatabase(application)
    private val qrCodeDao = database.qrCodeDao()

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    fun onQRCodeDetected(content: String, type: String) {
        viewModelScope.launch {
            try {
                val scan = QRCodeScan(
                    content = content,
                    type = type,
                    timestamp = Date()
                )
                qrCodeDao.insert(scan)
                _state.value = _state.value.copy(
                    lastScannedCode = scan,
                    error = null
                )
                
                // Provide feedback
                provideHapticFeedback()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to save scan: ${e.message}"
                )
            }
        }
    }

    fun clearLastScannedCode() {
        _state.value = _state.value.copy(
            lastScannedCode = null,
            error = null
        )
    }

    fun toggleFlashlight() {
        _state.value = _state.value.copy(
            isFlashlightOn = !_state.value.isFlashlightOn
        )
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
} 