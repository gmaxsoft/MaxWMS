package pl.maxwms.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

enum class BillingMessage {
    UNAVAILABLE,
    CANCELLED,
    ALREADY_OWNED,
    ERROR,
}

data class BillingUi(
    val ready: Boolean = false,
    val subscribed: Boolean = false,
    val price: String? = null,
    val message: BillingMessage? = null,
)

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connecting = AtomicBoolean(false)
    private var productDetails: ProductDetails? = null

    private val stateFlow = MutableStateFlow(BillingUi())
    val state: StateFlow<BillingUi> = stateFlow.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                val list = purchases.orEmpty()
                list.forEach(::acknowledgeIfNeeded)
                stateFlow.update {
                    it.copy(
                        subscribed = list.any(::isActiveSubscription),
                        ready = true,
                        message = null,
                    )
                }
            }
            BillingResponseCode.USER_CANCELED -> stateFlow.update { it.copy(message = BillingMessage.CANCELLED) }
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                stateFlow.update { it.copy(message = BillingMessage.ALREADY_OWNED) }
                queryPurchases()
            }
            else -> stateFlow.update { it.copy(message = BillingMessage.ERROR, ready = true) }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        scope.launch {
            delay(READY_TIMEOUT_MS)
            stateFlow.update { current ->
                if (current.ready) current else current.copy(ready = true, message = BillingMessage.UNAVAILABLE)
            }
        }
        connect()
    }

    fun refresh() {
        if (billingClient.isReady) {
            queryProduct()
            queryPurchases()
        } else {
            connect()
        }
    }

    fun clearMessage() {
        stateFlow.update { it.copy(message = null) }
    }

    fun launchPurchase(activity: Activity) {
        val product = productDetails
        val offerToken = product?.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (product == null || offerToken.isNullOrBlank()) {
            queryProduct()
            stateFlow.update { it.copy(message = BillingMessage.UNAVAILABLE) }
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) {
            stateFlow.update { it.copy(message = BillingMessage.ERROR) }
        }
    }

    private fun connect() {
        if (billingClient.isReady) {
            refresh()
            return
        }
        if (!connecting.compareAndSet(false, true)) return
        billingClient.startConnection(
            object : com.android.billingclient.api.BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    connecting.set(false)
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        queryProduct()
                        queryPurchases()
                    } else {
                        stateFlow.update {
                            it.copy(ready = true, subscribed = false, message = BillingMessage.UNAVAILABLE)
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    connecting.set(false)
                }
            },
        )
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingProducts.PRO_MONTHLY)
                        .setProductType(ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, detailsResult ->
            if (billingResult.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            val product = detailsResult.productDetailsList.firstOrNull()
            productDetails = product
            val price = product
                ?.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull { it.priceAmountMicros > 0 }
                ?.formattedPrice
                ?: product?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            stateFlow.update { it.copy(price = price, ready = true) }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                purchases.forEach(::acknowledgeIfNeeded)
                stateFlow.update {
                    it.copy(
                        subscribed = purchases.any(::isActiveSubscription),
                        ready = true,
                    )
                }
            } else {
                stateFlow.update { it.copy(ready = true) }
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { }
    }

    private fun isActiveSubscription(purchase: Purchase): Boolean {
        return purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            purchase.products.contains(BillingProducts.PRO_MONTHLY)
    }

    private companion object {
        const val READY_TIMEOUT_MS = 8_000L
    }
}
