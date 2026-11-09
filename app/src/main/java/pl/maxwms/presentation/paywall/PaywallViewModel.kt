package pl.maxwms.presentation.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.maxwms.data.billing.BillingManager
import pl.maxwms.data.billing.BillingMessage
import pl.maxwms.domain.model.UnlockResult
import pl.maxwms.domain.usecase.UnlockLicenseUseCase
import javax.inject.Inject

enum class PaywallError {
    INVALID,
    NETWORK,
    EMPTY,
    BILLING,
    UNAVAILABLE,
    OWNED,
}

data class PaywallUiState(
    val price: String? = null,
    val loading: Boolean = false,
    val error: PaywallError? = null,
    val showCodeDialog: Boolean = false,
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val unlockLicense: UnlockLicenseUseCase,
) : ViewModel() {
    private val loading = MutableStateFlow(false)
    private val localError = MutableStateFlow<PaywallError?>(null)
    private val dialog = MutableStateFlow(false)

    val uiState = combine(
        billingManager.state,
        loading,
        localError,
        dialog,
    ) { billing, isLoading, error, showDialog ->
        PaywallUiState(
            price = billing.price,
            loading = isLoading,
            error = error ?: billing.message.toPaywallError(),
            showCodeDialog = showDialog,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaywallUiState())

    fun subscribe(launch: () -> Unit) {
        localError.value = null
        billingManager.clearMessage()
        launch()
    }

    fun launchBilling(activity: android.app.Activity) {
        billingManager.launchPurchase(activity)
    }

    fun openCodeDialog() {
        localError.value = null
        dialog.value = true
    }

    fun dismissCodeDialog() {
        dialog.value = false
    }

    fun submitCode(code: String, saveAsApiKey: Boolean) {
        viewModelScope.launch {
            loading.value = true
            localError.value = null
            when (val result = unlockLicense(code, saveAsApiKey)) {
                is UnlockResult.Success -> dialog.value = false
                UnlockResult.Invalid -> localError.value = PaywallError.INVALID
                UnlockResult.Network -> localError.value = PaywallError.NETWORK
                UnlockResult.Empty -> localError.value = PaywallError.EMPTY
            }
            loading.value = false
        }
    }

    private fun BillingMessage?.toPaywallError(): PaywallError? = when (this) {
        BillingMessage.UNAVAILABLE -> PaywallError.UNAVAILABLE
        BillingMessage.ERROR -> PaywallError.BILLING
        BillingMessage.ALREADY_OWNED -> PaywallError.OWNED
        BillingMessage.CANCELLED, null -> null
    }
}
