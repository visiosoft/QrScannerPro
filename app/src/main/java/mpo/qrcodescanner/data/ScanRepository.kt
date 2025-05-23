package mpo.qrcodescanner.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanResultDao: ScanResultDao) {
    val allScans: Flow<List<ScanResult>> = scanResultDao.getAllScans()
    val favoriteScans: Flow<List<ScanResult>> = scanResultDao.getFavoriteScans()

    suspend fun insert(scanResult: ScanResult) {
        scanResultDao.insert(scanResult)
    }

    suspend fun delete(scanResult: ScanResult) {
        scanResultDao.delete(scanResult)
    }

    suspend fun update(scanResult: ScanResult) {
        scanResultDao.update(scanResult)
    }

    suspend fun deleteAll() {
        scanResultDao.deleteAll()
    }
} 