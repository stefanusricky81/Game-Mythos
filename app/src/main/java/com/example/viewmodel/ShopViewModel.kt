package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MythosConfig
import com.example.data.Card
import com.example.monetization.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ShopTab(val label: String) {
    FEATURED("Featured"),
    GEMS("Myth Gems"),
    CARDS("Cards"),
    SUMMON("Summon"),
    COSMETICS("Cosmetics")
}

data class ShopUiState(
    val selectedTab: ShopTab = ShopTab.FEATURED,
    val isLoading: Boolean = false,
    val simulationMode: MockBillingResult = MockBillingResult.SUCCESS,
    val successPurchase: PurchaseResult.Success? = null,
    val gemSuccessProduct: GemProduct? = null,
    val statusNotification: String? = null,
    val isErrorNotification: Boolean = false,
    val summonResults: List<SummonedCardResult>? = null,
    val showSummonRatesDialog: Boolean = false,
    val showHistoryDialog: Boolean = false
)

class ShopViewModel(
    private val repository: PlayerEconomyRepository = PlayerEconomyRepository.instance
) : ViewModel() {

    val economyState: StateFlow<PlayerEconomyState> = repository.economyState

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    fun selectTab(tab: ShopTab) {
        _uiState.update { it.copy(selectedTab = tab, statusNotification = null) }
    }

    fun setSimulationMode(mode: MockBillingResult) {
        _uiState.update { it.copy(simulationMode = mode) }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(successPurchase = null, gemSuccessProduct = null) }
    }

    fun dismissSummonDialog() {
        _uiState.update { it.copy(summonResults = null) }
    }

    fun showSummonRates(show: Boolean) {
        _uiState.update { it.copy(showSummonRatesDialog = show) }
    }

    fun showHistory(show: Boolean) {
        _uiState.update { it.copy(showHistoryDialog = show) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(statusNotification = null) }
    }

    fun buyGemProduct(product: GemProduct) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusNotification = null) }
            val result = repository.purchaseGemProduct(product, _uiState.value.simulationMode)
            _uiState.update { it.copy(isLoading = false) }

            handlePurchaseResult(result, isGemProduct = true, gemProduct = product)
        }
    }

    fun buyBundle(bundle: PremiumBundle) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusNotification = null) }
            val result = repository.purchaseBundle(bundle, _uiState.value.simulationMode)
            _uiState.update { it.copy(isLoading = false) }

            handlePurchaseResult(result)
        }
    }

    fun buyDirectCard(card: Card) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusNotification = null) }
            val result = repository.purchaseDirectCard(card, _uiState.value.simulationMode)
            _uiState.update { it.copy(isLoading = false) }

            handlePurchaseResult(result)
        }
    }

    fun buyCosmetic(cosmetic: CosmeticItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusNotification = null) }
            val result = repository.purchaseCosmetic(cosmetic, _uiState.value.simulationMode)
            _uiState.update { it.copy(isLoading = false) }

            handlePurchaseResult(result)
        }
    }

    fun executeSummon(count: Int) {
        val (success, pulledCards) = repository.performSummon(count)
        if (success) {
            _uiState.update { it.copy(summonResults = pulledCards) }
        } else {
            _uiState.update {
                it.copy(
                    statusNotification = "Insufficient Myth Gems for summon! Top up in the Gem Shop.",
                    isErrorNotification = true
                )
            }
        }
    }

    private fun handlePurchaseResult(
        result: PurchaseResult,
        isGemProduct: Boolean = false,
        gemProduct: GemProduct? = null
    ) {
        when (result) {
            is PurchaseResult.Success -> {
                if (isGemProduct && gemProduct != null) {
                    _uiState.update { it.copy(gemSuccessProduct = gemProduct, statusNotification = null) }
                } else {
                    _uiState.update { it.copy(successPurchase = result, statusNotification = null) }
                }
            }
            is PurchaseResult.Cancelled -> {
                _uiState.update {
                    it.copy(
                        statusNotification = "Purchase cancelled.",
                        isErrorNotification = false
                    )
                }
            }
            is PurchaseResult.Pending -> {
                _uiState.update {
                    it.copy(
                        statusNotification = "Purchase is pending verification. Rewards will appear once confirmed.",
                        isErrorNotification = false
                    )
                }
            }
            is PurchaseResult.Failed -> {
                _uiState.update {
                    it.copy(
                        statusNotification = result.message,
                        isErrorNotification = true
                    )
                }
            }
        }
    }

    // DEVELOPMENT CONTROLS (Requirement #36)
    fun debugAddGems(amount: Int) {
        repository.debugAddGems(amount)
        _uiState.update { it.copy(statusNotification = "Debug: Added $amount Myth Gems", isErrorNotification = false) }
    }

    fun debugAddGold(amount: Int) {
        repository.debugAddGold(amount)
        _uiState.update { it.copy(statusNotification = "Debug: Added $amount Gold", isErrorNotification = false) }
    }

    fun debugResetEconomy() {
        repository.debugResetEconomy()
        _uiState.update { it.copy(statusNotification = "Debug: Reset economy & purchases", isErrorNotification = false) }
    }
}
