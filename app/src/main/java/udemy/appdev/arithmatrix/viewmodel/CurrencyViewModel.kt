package udemy.appdev.arithmatrix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import udemy.appdev.arithmatrix.data.local.HistoryEntity
import udemy.appdev.arithmatrix.data.repository.CurrencyRepository
import udemy.appdev.arithmatrix.data.repository.HistoryRepository
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val repository: CurrencyRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val supportedCurrencies: List<String> = repository.getSupportedCurrencies()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount

    private val _fromCurrency = MutableStateFlow("USD")
    val fromCurrency: StateFlow<String> = _fromCurrency

    private val _toCurrency = MutableStateFlow("INR")
    val toCurrency: StateFlow<String> = _toCurrency

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _convertedText = MutableStateFlow("")
    val convertedText: StateFlow<String> = _convertedText

    fun onAmountChanged(newAmount: String) { _amount.value = newAmount }
    fun onFromCurrencyChanged(currency: String) { _fromCurrency.value = currency }
    fun onToCurrencyChanged(currency: String) { _toCurrency.value = currency }

    fun swapCurrencies() {
        val oldFrom = _fromCurrency.value
        _fromCurrency.value = _toCurrency.value
        _toCurrency.value = oldFrom
    }

    fun convert() {
        val amt = amount.value.toDoubleOrNull()
        if (amt == null || amt <= 0.0) {
            _error.value = "Please enter a valid amount"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val resultValue = repository.convertCurrency(amt, fromCurrency.value, toCurrency.value)
                val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
                val formattedAmt = formatter.format(amt)
                val formattedResult = formatter.format(resultValue)

                _result.value = formattedResult
                _convertedText.value = "$formattedAmt ${fromCurrency.value} = $formattedResult ${toCurrency.value}"

                historyRepository.insert(
                    HistoryEntity(
                        expression = "$formattedAmt ${fromCurrency.value} → ${toCurrency.value}",
                        result = "$formattedResult ${toCurrency.value}",
                        source = "CURRENCY",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Conversion failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
}