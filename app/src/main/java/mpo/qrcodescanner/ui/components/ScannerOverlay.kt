package mpo.qrcodescanner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier
) {
    val animatedProgress = rememberInfiniteTransition(label = "scan_line").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_line"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Scanner window size (70% of the smaller dimension)
        val scannerSize = minOf(width, height) * 0.7f
        val scannerRect = Size(scannerSize, scannerSize)
        
        // Center position for scanner window
        val scannerOffset = Offset(
            (width - scannerSize) / 2f,
            (height - scannerSize) / 2f
        )

        // Draw semi-transparent overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size,
        )

        // Cut out the scanner window
        drawRoundRect(
            color = Color.Transparent,
            topLeft = scannerOffset,
            size = scannerRect,
            cornerRadius = CornerRadius(12.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // Draw scanner frame
        drawRoundRect(
            color = Color.White,
            topLeft = scannerOffset,
            size = scannerRect,
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(width = 4.dp.toPx())
        )

        // Draw scanning line
        val scanLineY = scannerOffset.y + (scannerSize * animatedProgress.value)
        if (scanLineY <= scannerOffset.y + scannerSize) {
            drawLine(
                color = Color(0xFF00E676),
                start = Offset(scannerOffset.x, scanLineY),
                end = Offset(scannerOffset.x + scannerSize, scanLineY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
            )
        }

        // Draw corner indicators
        val cornerLength = 20.dp.toPx()
        val cornerWidth = 3.dp.toPx()
        val corners = listOf(
            // Top-left
            Pair(
                Offset(scannerOffset.x, scannerOffset.y) to Offset(scannerOffset.x + cornerLength, scannerOffset.y),
                Offset(scannerOffset.x, scannerOffset.y) to Offset(scannerOffset.x, scannerOffset.y + cornerLength)
            ),
            // Top-right
            Pair(
                Offset(scannerOffset.x + scannerSize - cornerLength, scannerOffset.y) to Offset(scannerOffset.x + scannerSize, scannerOffset.y),
                Offset(scannerOffset.x + scannerSize, scannerOffset.y) to Offset(scannerOffset.x + scannerSize, scannerOffset.y + cornerLength)
            ),
            // Bottom-left
            Pair(
                Offset(scannerOffset.x, scannerOffset.y + scannerSize) to Offset(scannerOffset.x + cornerLength, scannerOffset.y + scannerSize),
                Offset(scannerOffset.x, scannerOffset.y + scannerSize - cornerLength) to Offset(scannerOffset.x, scannerOffset.y + scannerSize)
            ),
            // Bottom-right
            Pair(
                Offset(scannerOffset.x + scannerSize - cornerLength, scannerOffset.y + scannerSize) to Offset(scannerOffset.x + scannerSize, scannerOffset.y + scannerSize),
                Offset(scannerOffset.x + scannerSize, scannerOffset.y + scannerSize - cornerLength) to Offset(scannerOffset.x + scannerSize, scannerOffset.y + scannerSize)
            )
        )

        corners.forEach { (line1, line2) ->
            drawLine(
                color = Color(0xFF00E676),
                start = line1.first,
                end = line1.second,
                strokeWidth = cornerWidth
            )
            drawLine(
                color = Color(0xFF00E676),
                start = line2.first,
                end = line2.second,
                strokeWidth = cornerWidth
            )
        }
    }
} 