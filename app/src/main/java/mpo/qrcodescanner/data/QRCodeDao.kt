package mpo.qrcodescanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QRCodeDao {
    @Query("SELECT * FROM qr_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<QRCodeScan>>

    @Query("SELECT * FROM qr_scans WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScans(): Flow<List<QRCodeScan>>

    @Insert
    suspend fun insert(scan: QRCodeScan)

    @Update
    suspend fun update(scan: QRCodeScan)

    @Delete
    suspend fun delete(scan: QRCodeScan)

    @Query("DELETE FROM qr_scans")
    suspend fun deleteAll()
} 