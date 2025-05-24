package mpo.qrcodescanner

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mpo.qrcodescanner.ads.AdsManager
import mpo.qrcodescanner.ui.home.HomeScreen
import mpo.qrcodescanner.ui.scanner.ScannerScreen
import mpo.qrcodescanner.ui.scanner.ScannerViewModel
import mpo.qrcodescanner.ui.history.HistoryScreen
import mpo.qrcodescanner.ui.theme.QRCodeScannerTheme
import mpo.qrcodescanner.ui.subscription.SubscriptionScreen

class MainActivity : ComponentActivity() {
    private lateinit var adsManager: AdsManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Camera permission is required to scan QR codes",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        adsManager = AdsManager(this)

        setContent {
            QRCodeScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(Unit) {
                        adsManager.showInitialInterstitialAd(this@MainActivity)
                    }
                    
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onScanClick = { navController.navigate("scanner") },
                                onHistoryClick = { navController.navigate("history") },
                                onPremiumClick = { navController.navigate("subscription") },
                                onSettingsClick = { /* TODO: Implement settings navigation */ }
                            )
                        }
                        composable("scanner") {
                            val scannerViewModel: ScannerViewModel = viewModel()
                            ScannerScreen(
                                viewModel = scannerViewModel,
                                onPermissionDenied = {
                                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            )
                        }
                        composable("history") {
                            HistoryScreen()
                        }
                        composable("subscription") {
                            SubscriptionScreen(
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}