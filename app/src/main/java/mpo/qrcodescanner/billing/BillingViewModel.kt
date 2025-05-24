package mpo.qrcodescanner.billing

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "BillingViewModel"
    private val billingManager = BillingManager.getInstance(application)

    val isPremium: StateFlow<Boolean> = billingManager.isPremium.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        false
    )

    val error: StateFlow<String?> = billingManager.error.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        null
    )

    fun purchaseMonthlySubscription(activity: Activity) {
        Log.d(TAG, "Initiating monthly subscription purchase")
        viewModelScope.launch {
            try {
                billingManager.launchBillingFlow(activity, BillingManager.MONTHLY_SUB_ID)
            } catch (e: Exception) {
                Log.e(TAG, "Error purchasing monthly subscription", e)
                throw e
            }
        }
    }

    fun purchaseYearlySubscription(activity: Activity) {
        Log.d(TAG, "Initiating yearly subscription purchase")
        viewModelScope.launch {
            try {
                billingManager.launchBillingFlow(activity, BillingManager.YEARLY_SUB_ID)
            } catch (e: Exception) {
                Log.e(TAG, "Error purchasing yearly subscription", e)
                throw e
            }
        }
    }

    fun purchaseLifetime(activity: Activity) {
        Log.d(TAG, "Initiating lifetime purchase")
        viewModelScope.launch {
            try {
                billingManager.launchBillingFlow(activity, BillingManager.LIFETIME_PURCHASE_ID)
            } catch (e: Exception) {
                Log.e(TAG, "Error purchasing lifetime access", e)
                throw e
            }
        }
    }

    fun purchaseRemoveAds(activity: Activity) {
        Log.d(TAG, "Initiating remove ads purchase")
        viewModelScope.launch {
            try {
                billingManager.launchBillingFlow(activity, BillingManager.REMOVE_ADS_ID)
            } catch (e: Exception) {
                Log.e(TAG, "Error purchasing remove ads", e)
                throw e
            }
        }
    }

    fun clearError() {
        (billingManager.error as? kotlinx.coroutines.flow.MutableStateFlow)?.value = null
    }
} 