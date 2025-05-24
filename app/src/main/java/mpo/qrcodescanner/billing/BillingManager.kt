package mpo.qrcodescanner.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager private constructor(
    private val context: Context
) : PurchasesUpdatedListener, BillingClientStateListener {

    private val TAG = "BillingManager"
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    companion object {
        // Test product IDs for development
        const val MONTHLY_SUB_ID = "android.test.purchased"
        const val YEARLY_SUB_ID = "android.test.purchased"
        const val LIFETIME_PURCHASE_ID = "android.test.purchased"
        const val REMOVE_ADS_ID = "android.test.purchased"

        // Production product IDs (commented out for now)
        // const val MONTHLY_SUB_ID = "qr_scanner_monthly_sub"
        // const val YEARLY_SUB_ID = "qr_scanner_yearly_sub"
        // const val LIFETIME_PURCHASE_ID = "qr_scanner_lifetime"
        // const val REMOVE_ADS_ID = "qr_scanner_remove_ads"

        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context).also { instance = it }
            }
        }
    }

    init {
        Log.d(TAG, "Initializing BillingManager")
        connectToPlayBilling()
    }

    private fun connectToPlayBilling() {
        Log.d(TAG, "Connecting to Play Billing")
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d(TAG, "Billing setup finished. Response code: ${billingResult.responseCode}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _isConnected.postValue(true)
            queryPurchases()
        } else {
            _isConnected.postValue(false)
            _error.value = "Billing setup failed: ${billingResult.debugMessage}"
            Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.d(TAG, "Billing service disconnected. Attempting to reconnect...")
        _isConnected.postValue(false)
        connectToPlayBilling()
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        Log.d(TAG, "Purchases updated. Response code: ${billingResult.responseCode}")
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                } else {
                    Log.d(TAG, "No purchases found")
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled the purchase")
                _error.value = "Purchase canceled"
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                _error.value = "Purchase failed: ${billingResult.debugMessage}"
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "Handling purchase: ${purchase.products}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "Purchase acknowledged successfully")
                        } else {
                            Log.e(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error acknowledging purchase", e)
                    }
                }
            }
            _isPremium.value = true
            Log.d(TAG, "Premium status updated: ${_isPremium.value}")
        }
    }

    private fun queryPurchases() {
        Log.d(TAG, "Querying existing purchases")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val purchasesResult = billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
                val oneTimePurchasesResult = billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )

                Log.d(TAG, "Found ${purchasesResult.purchasesList.size} subscriptions and ${oneTimePurchasesResult.purchasesList.size} one-time purchases")

                val hasPremium = (purchasesResult.purchasesList + oneTimePurchasesResult.purchasesList)
                    .any { purchase ->
                        purchase.products.any { productId ->
                            productId in listOf(
                                MONTHLY_SUB_ID,
                                YEARLY_SUB_ID,
                                LIFETIME_PURCHASE_ID,
                                REMOVE_ADS_ID
                            )
                        } && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }

                _isPremium.value = hasPremium
                Log.d(TAG, "Premium status updated from query: $hasPremium")
            } catch (e: Exception) {
                Log.e(TAG, "Error querying purchases", e)
                _error.value = "Error querying purchases: ${e.message}"
            }
        }
    }

    suspend fun launchBillingFlow(activity: Activity, productId: String) {
        Log.d(TAG, "Launching billing flow for product: $productId")
        withContext(Dispatchers.IO) {
            try {
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(
                            when (productId) {
                                MONTHLY_SUB_ID, YEARLY_SUB_ID -> BillingClient.ProductType.SUBS
                                else -> BillingClient.ProductType.INAPP
                            }
                        )
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                Log.d(TAG, "Querying product details for: $productId")
                val productDetailsResult = billingClient.queryProductDetails(params)
                
                if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetails = productDetailsResult.productDetailsList?.firstOrNull()
                    
                    if (productDetails != null) {
                        Log.d(TAG, "Product details found: ${productDetails.productId}")
                        val offerToken = if (productDetails.productType == BillingClient.ProductType.SUBS) {
                            productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                        } else null

                        val productDetailsParamsList = listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .apply {
                                    if (offerToken != null) {
                                        setOfferToken(offerToken)
                                    }
                                }
                                .build()
                        )

                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build()

                        withContext(Dispatchers.Main) {
                            val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
                            Log.d(TAG, "Billing flow launched. Response code: ${billingResult.responseCode}")
                            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                                _error.value = "Failed to launch billing: ${billingResult.debugMessage}"
                            }
                        }
                    } else {
                        // For test purchases, simulate a successful purchase
                        if (productId == "android.test.purchased") {
                            Log.d(TAG, "Simulating successful test purchase")
                            _isPremium.value = true
                            _error.value = null
                        } else {
                            Log.e(TAG, "Product details not found for: $productId")
                            _error.value = "Product not found"
                        }
                    }
                } else {
                    // For test purchases, simulate a successful purchase
                    if (productId == "android.test.purchased") {
                        Log.d(TAG, "Simulating successful test purchase")
                        _isPremium.value = true
                        _error.value = null
                    } else {
                        Log.e(TAG, "Failed to query product details: ${productDetailsResult.billingResult.debugMessage}")
                        _error.value = "Failed to get product details: ${productDetailsResult.billingResult.debugMessage}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in launchBillingFlow", e)
                _error.value = "Error launching billing: ${e.message}"
            }
        }
    }
} 