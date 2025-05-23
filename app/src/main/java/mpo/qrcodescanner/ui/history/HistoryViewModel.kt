package mpo.qrcodescanner.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mpo.qrcodescanner.data.AppDatabase
import mpo.qrcodescanner.data.ScanRepository
import mpo.qrcodescanner.data.ScanResult

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScanRepository
    val allScans: Flow<List<ScanResult>>

    init {
        val dao = AppDatabase.getDatabase(application).scanResultDao()
        repository = ScanRepository(dao)
        allScans = repository.allScans
    }

    fun delete(scan: ScanResult) = viewModelScope.launch {
        repository.delete(scan)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }

    fun toggleFavorite(scan: ScanResult) = viewModelScope.launch {
        repository.update(scan.copy(isFavorite = !scan.isFavorite))
    }
} 