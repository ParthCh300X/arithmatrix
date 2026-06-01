package udemy.appdev.arithmatrix.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import udemy.appdev.arithmatrix.data.RetrofitInstance
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "currency_cache")

class CurrencyRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val supportedCurrencies = listOf(
        "USD", "EUR", "INR", "GBP", "JPY", "CAD", "AUD", "CNY"
    )

    fun getSupportedCurrencies(): List<String> = supportedCurrencies

    suspend fun convertCurrency(amount: Double, from: String, to: String): Double {
        return withContext(Dispatchers.IO) {
            val rates = getRatesWithCache(from)
            val rate = rates[to] ?: throw Exception("Rate for $to not found")
            amount * rate
        }
    }

    private suspend fun getRatesWithCache(base: String): Map<String, Double> {
        val ratesKey = stringPreferencesKey("rates_$base")
        val timestampKey = longPreferencesKey("timestamp_$base")
        val oneHourMs = 60 * 60 * 1000L

        val prefs = context.dataStore.data.first()
        val cachedRates = prefs[ratesKey]
        val cachedTime = prefs[timestampKey] ?: 0L

        val now = System.currentTimeMillis()

        // Return cached rates if less than 1 hour old
        if (cachedRates != null && (now - cachedTime) < oneHourMs) {
            return parseRatesJson(cachedRates)
        }

        // Fetch fresh from network
        val response = RetrofitInstance.api.getRatesForBase(base)
        val ratesJson = JSONObject(response.rates as Map<*, *>).toString()

        context.dataStore.edit { settings ->
            settings[ratesKey] = ratesJson
            settings[timestampKey] = now
        }

        return response.rates
    }

    private fun parseRatesJson(json: String): Map<String, Double> {
        val obj = JSONObject(json)
        return obj.keys().asSequence().associateWith { obj.getDouble(it) }
    }
}