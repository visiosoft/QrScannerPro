package mpo.qrcodescanner.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val billingManager = BillingManager.getInstance(application)

    val isPremium: StateFlow<Boolean> = billingManager.isPremium.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        false
    )

    fun purchaseMonthlySubscription(activity: Activity) {
        viewModelScope.launch {
            billingManager.launchBillingFlow(activity, BillingManager.MONTHLY_SUB_ID)
        }
    }

    fun purchaseYearlySubscription(activity: Activity) {
        viewModelScope.launch {
            billingManager.launchBillingFlow(activity, BillingManager.YEARLY_SUB_ID)
        }
    }

    fun purchaseLifetime(activity: Activity) {
        viewModelScope.launch {
            billingManager.launchBillingFlow(activity, BillingManager.LIFETIME_PURCHASE_ID)
        }
    }

    fun purchaseRemoveAds(activity: Activity) {
        viewModelScope.launch {
            billingManager.launchBillingFlow(activity, BillingManager.REMOVE_ADS_ID)
        }
    }
} 