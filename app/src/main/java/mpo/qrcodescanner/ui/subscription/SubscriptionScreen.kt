package mpo.qrcodescanner.ui.subscription

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import mpo.qrcodescanner.billing.BillingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    billingViewModel: BillingViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isPremium by billingViewModel.isPremium.collectAsState()
    val error by billingViewModel.error.collectAsState()
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(error) {
        error?.let {
            snackbarMessage = it
            showSnackbar = true
            delay(3000)
            showSnackbar = false
            billingViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Features") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isPremium) {
                Text(
                    "You're a Premium User!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Thank you for supporting us. Enjoy all premium features!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    "Upgrade to Premium",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                SubscriptionCard(
                    title = "Monthly Subscription",
                    price = "$1.99/month",
                    features = listOf(
                        "Ad-free experience",
                        "Dynamic QR codes",
                        "Advanced analytics",
                        "Priority support"
                    ),
                    onClick = { 
                        try {
                            billingViewModel.purchaseMonthlySubscription(context as android.app.Activity)
                        } catch (e: Exception) {
                            snackbarMessage = "Error: ${e.message}"
                            showSnackbar = true
                        }
                    }
                )

                SubscriptionCard(
                    title = "Yearly Subscription",
                    price = "$9.99/year",
                    features = listOf(
                        "All monthly features",
                        "Save 58% compared to monthly",
                        "Exclusive yearly member benefits"
                    ),
                    onClick = { 
                        try {
                            billingViewModel.purchaseYearlySubscription(context as android.app.Activity)
                        } catch (e: Exception) {
                            snackbarMessage = "Error: ${e.message}"
                            showSnackbar = true
                        }
                    }
                )

                SubscriptionCard(
                    title = "Lifetime Access",
                    price = "$14.99 (one-time)",
                    features = listOf(
                        "All premium features forever",
                        "One-time payment",
                        "Best value for long-term use"
                    ),
                    onClick = { 
                        try {
                            billingViewModel.purchaseLifetime(context as android.app.Activity)
                        } catch (e: Exception) {
                            snackbarMessage = "Error: ${e.message}"
                            showSnackbar = true
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                OutlinedButton(
                    onClick = { 
                        try {
                            billingViewModel.purchaseRemoveAds(context as android.app.Activity)
                        } catch (e: Exception) {
                            snackbarMessage = "Error: ${e.message}"
                            showSnackbar = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove Ads Only - $2.99")
                }
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    title: String,
    price: String,
    features: List<String>,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = price,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            features.forEach { feature ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(feature)
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Subscribe")
            }
        }
    }
} 