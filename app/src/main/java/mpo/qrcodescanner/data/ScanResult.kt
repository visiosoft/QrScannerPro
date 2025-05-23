package mpo.qrcodescanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "scan_results")
data class ScanResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val type: String,
    val timestamp: Date = Date(),
    val isFavorite: Boolean = false
) 