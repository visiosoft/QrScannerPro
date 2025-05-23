package mpo.qrcodescanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanResult>>

    @Query("SELECT * FROM scan_results WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScans(): Flow<List<ScanResult>>

    @Insert
    suspend fun insert(scanResult: ScanResult)

    @Delete
    suspend fun delete(scanResult: ScanResult)

    @Update
    suspend fun update(scanResult: ScanResult)

    @Query("DELETE FROM scan_results")
    suspend fun deleteAll()
} 