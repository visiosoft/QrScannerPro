package mpo.qrcodescanner.billing

import android.app.Activity
import android.content.Context
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

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    companion object {
        const val MONTHLY_SUB_ID = "qr_scanner_monthly_sub"
        const val YEARLY_SUB_ID = "qr_scanner_yearly_sub"
        const val LIFETIME_PURCHASE_ID = "qr_scanner_lifetime"
        const val REMOVE_ADS_ID = "qr_scanner_remove_ads"

        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context).also { instance = it }
            }
        }
    }

    init {
        connectToPlayBilling()
    }

    private fun connectToPlayBilling() {
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _isConnected.postValue(true)
            queryPurchases()
        } else {
            _isConnected.postValue(false)
        }
    }

    override fun onBillingServiceDisconnected() {
        _isConnected.postValue(false)
        connectToPlayBilling()
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                CoroutineScope(Dispatchers.IO).launch {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                }
            }
            _isPremium.value = true
        }
    }

    private fun queryPurchases() {
        CoroutineScope(Dispatchers.IO).launch {
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
        }
    }

    suspend fun launchBillingFlow(activity: Activity, productId: String) {
        withContext(Dispatchers.IO) {
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

            val productDetailsResult = billingClient.queryProductDetails(params)
            
            if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetails = productDetailsResult.productDetailsList?.firstOrNull()
                
                if (productDetails != null) {
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
                        billingClient.launchBillingFlow(activity, billingFlowParams)
                    }
                }
            }
        }
    }
} 