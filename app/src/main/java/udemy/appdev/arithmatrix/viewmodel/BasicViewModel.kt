package udemy.appdev.arithmatrix.ui.basic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import udemy.appdev.arithmatrix.data.local.HistoryEntity
import udemy.appdev.arithmatrix.data.repository.HistoryRepository
import udemy.appdev.arithmatrix.engine.CalculatorEngine
import udemy.appdev.arithmatrix.widget.updateWidget
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class BasicViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val historyRepository: HistoryRepository,
    private val calculatorEngine: CalculatorEngine
) : ViewModel() {

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result

    private val _isScientific = MutableStateFlow(false)
    val isScientific: StateFlow<Boolean> = _isScientific

    // Tracks the base for ^ — user types base, taps ^, then types exponent
    private var pendingPowerBase: Double? = null

    private var justEvaluated = false

    fun toggleScientific() {
        _isScientific.value = !_isScientific.value
    }

    fun onInput(input: String) {
        when (input) {
            "AC" -> {
                _expression.value = ""
                _result.value = ""
                justEvaluated = false
                pendingPowerBase = null
            }

            "⌫" -> {
                if (!justEvaluated && _expression.value.isNotEmpty()) {
                    _expression.value = _expression.value.dropLast(1)
                }
                justEvaluated = false
            }

            "=" -> {
                if (_expression.value.isEmpty()) return

                // If user was in the middle of a ^ operation, complete it
                if (pendingPowerBase != null) {
                    val exponent = _expression.value.toDoubleOrNull()
                    if (exponent == null) {
                        _result.value = "Error"
                        pendingPowerBase = null
                        return
                    }
                    val res = calculatorEngine.formatResult(pendingPowerBase!!.pow(exponent))
                    val exprDisplay = "${pendingPowerBase}^${exponent}"
                    _result.value = res
                    _expression.value = exprDisplay
                    justEvaluated = true
                    pendingPowerBase = null
                    viewModelScope.launch {
                        historyRepository.insert(
                            HistoryEntity(
                                expression = exprDisplay,
                                result = res,
                                source = "BASIC",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    viewModelScope.launch {
                        updateWidget(context = appContext, expression = exprDisplay, result = res)
                    }
                    return
                }

                try {
                    val raw = calculatorEngine.evaluate(_expression.value)
                    val res = calculatorEngine.formatResult(raw)
                    _result.value = res
                    justEvaluated = true
                    viewModelScope.launch {
                        historyRepository.insert(
                            HistoryEntity(
                                expression = _expression.value,
                                result = res,
                                source = "BASIC",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    viewModelScope.launch {
                        updateWidget(
                            context    = appContext,
                            expression = _expression.value,
                            result     = res
                        )
                    }
                } catch (e: Exception) {
                    _result.value = "Error"
                    justEvaluated = false
                }
            }

            "+", "-", "×", "÷" -> {
                if (justEvaluated && _result.value.isNotEmpty() && _result.value != "Error") {
                    _expression.value = _result.value + input
                    _result.value = ""
                } else {
                    _expression.value += input
                }
                justEvaluated = false
            }

            // ^ — store current value as base, clear expression for exponent entry
            "^" -> {
                val base = if (justEvaluated && _result.value.isNotEmpty()) {
                    _result.value.toDoubleOrNull()
                } else {
                    try { calculatorEngine.evaluate(_expression.value) } catch (e: Exception) { null }
                }
                if (base == null) {
                    _result.value = "Error"
                    return
                }
                pendingPowerBase = base
                _result.value = "$base ^"   // show user what's happening
                _expression.value = ""
                justEvaluated = false
            }

            // All other scientific functions — evaluate immediately
            "sin", "cos", "tan", "√", "x²", "log", "ln" -> {
                val operand = if (justEvaluated && _result.value.isNotEmpty()) {
                    _result.value.toDoubleOrNull()
                } else {
                    try { calculatorEngine.evaluate(_expression.value) } catch (e: Exception) { null }
                }

                if (operand == null) {
                    _result.value = "Error"
                    return
                }

                val sciResult = when (input) {
                    "sin" -> sin(Math.toRadians(operand))
                    "cos" -> cos(Math.toRadians(operand))
                    "tan" -> tan(Math.toRadians(operand))
                    "√"   -> if (operand < 0) { _result.value = "Error"; return } else sqrt(operand)
                    "x²"  -> operand.pow(2)
                    "log" -> if (operand <= 0) { _result.value = "Error"; return } else log10(operand)
                    "ln"  -> if (operand <= 0) { _result.value = "Error"; return } else ln(operand)
                    else  -> operand
                }

                val formatted = calculatorEngine.formatResult(sciResult)
                _expression.value = "$input($operand)"
                _result.value = formatted
                justEvaluated = true

                viewModelScope.launch {
                    historyRepository.insert(
                        HistoryEntity(
                            expression = "$input($operand)",
                            result = formatted,
                            source = "BASIC",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            "%" -> {
                _expression.value += "%"
                justEvaluated = false
            }

            else -> {
                if (justEvaluated) {
                    _expression.value = input
                    _result.value = ""
                    justEvaluated = false
                } else {
                    _expression.value += input
                }
            }
        }
    }

    fun loadExpression(expr: String) {
        _expression.value = expr
        _result.value = ""
        justEvaluated = false
        pendingPowerBase = null
    }
}