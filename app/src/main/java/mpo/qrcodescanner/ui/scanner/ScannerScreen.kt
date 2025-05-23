package mpo.qrcodescanner.ui.scanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import mpo.qrcodescanner.ui.components.ScannerOverlay
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = viewModel(),
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val state by viewModel.state.collectAsState()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            onPermissionDenied()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                onQRCodeDetected = { content, type ->
                    viewModel.onQRCodeDetected(content, type)
                },
                isFlashlightOn = state.isFlashlightOn,
                executor = cameraExecutor
            )
            
            // Scanner overlay
            ScannerOverlay()
            
            // Flashlight toggle
            FilledIconButton(
                onClick = { viewModel.toggleFlashlight() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = if (state.isFlashlightOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    contentDescription = if (state.isFlashlightOn) "Turn off flashlight" else "Turn on flashlight",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        state.lastScannedCode?.let { scan ->
            AlertDialog(
                onDismissRequest = { viewModel.clearLastScannedCode() },
                title = { Text("QR Code Detected") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Type: ${scan.type}")
                        Text("Content: ${scan.content}")
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Copy button
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("QR Code Content", scan.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Content copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy")
                        }

                        // Continue button
                        TextButton(
                            onClick = { viewModel.clearLastScannedCode() }
                        ) {
                            Text("Continue Scanning")
                        }
                    }
                }
            )
        }

        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(error)
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onQRCodeDetected: (String, String) -> Unit,
    isFlashlightOn: Boolean,
    executor: java.util.concurrent.Executor
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val scanner = remember { BarcodeScanning.getClient() }
    var camera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(isFlashlightOn) {
        camera?.cameraControl?.enableTorch(isFlashlightOn)
    }

    LaunchedEffect(previewView) {
        val cameraProvider = suspendCoroutine<ProcessCameraProvider> { continuation ->
            ProcessCameraProvider.getInstance(context).also { future ->
                future.addListener(
                    { continuation.resume(future.get()) },
                    ContextCompat.getMainExecutor(context)
                )
            }
        }

        val preview = Preview.Builder()
            .setTargetResolution(Size(1280, 720))
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull()?.let { barcode ->
                                    barcode.rawValue?.let { value ->
                                        val type = when (barcode.valueType) {
                                            Barcode.TYPE_URL -> "URL"
                                            Barcode.TYPE_CONTACT_INFO -> "Contact"
                                            Barcode.TYPE_WIFI -> "WiFi"
                                            else -> "Text"
                                        }
                                        onQRCodeDetected(value, type)
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
} 