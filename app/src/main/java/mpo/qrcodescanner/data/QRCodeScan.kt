package mpo.qrcodescanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "qr_scans")
data class QRCodeScan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val type: String,
    val timestamp: Date,
    val isFavorite: Boolean = false
) 