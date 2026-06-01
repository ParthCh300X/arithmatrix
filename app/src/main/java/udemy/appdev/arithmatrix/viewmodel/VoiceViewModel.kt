package udemy.appdev.arithmatrix.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repo: HistoryRepository,
    private val calculatorEngine: CalculatorEngine
) : ViewModel() {

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _ttsEnabled = MutableStateFlow(true)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    private val recognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { _isListening.value = false }

                override fun onPartialResults(partialResults: Bundle?) {
                    val list = partialResults?.getCharSequenceArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!list.isNullOrEmpty()) _expression.value = list[0].toString()
                }

                override fun onResults(results: Bundle) {
                    _isListening.value = false
                    val list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = list?.firstOrNull().orEmpty()
                    handleFinalText(spoken)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

    fun toggleTts() { _ttsEnabled.value = !_ttsEnabled.value }

    fun startListening() {
        if (_isListening.value) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        _expression.value = ""
        _result.value = ""
        recognizer.startListening(intent)
        _isListening.value = true
    }

    fun stopListening() {
        recognizer.stopListening()
        _isListening.value = false
    }

    private fun handleFinalText(text: String) {
        _expression.value = text
        val normalized = normalize(text)
        val res = try {
            val raw = calculatorEngine.evaluate(normalized)
            calculatorEngine.formatResult(raw)
        } catch (_: Exception) {
            "Error"
        }
        _result.value = res

        viewModelScope.launch {
            repo.insert(HistoryEntity(expression = text, result = res, source = "VOICE"))
        }
        if (_ttsEnabled.value && res != "Error") speak(res)
    }

    private fun normalize(raw: String): String {
        return raw.lowercase(Locale.getDefault())
            .let { wordsToNumbers(it) }
            // scientific phrase mapping — engine now handles these as inline expressions
            .replace(Regex("sine?\\s*of\\s*"), "sin(").let { if (it.contains("sin(")) "$it)" else it }
            .replace(Regex("cosine?\\s*of\\s*"), "cos(").let { if (it.contains("cos(")) "$it)" else it }
            .replace(Regex("tan(?:gent)?\\s*of\\s*"), "tan(").let { if (it.contains("tan(")) "$it)" else it }
            .replace(Regex("(?:square\\s*root|sqrt)\\s*of\\s*"), "sqrt(").let { if (it.contains("sqrt(")) "$it)" else it }
            .replace(Regex("log\\s*of\\s*"), "log(").let { if (it.contains("log(")) "$it)" else it }
            .replace(Regex("ln\\s*of\\s*"), "ln(").let { if (it.contains("ln(")) "$it)" else it }
            // operator aliases
            .replace(Regex("\\bx\\b"), "*")
            .replace("×", "*")
            .replace("times", "*")
            .replace("into", "*")
            .replace("divided by", "/")
            .replace("divide by", "/")
            .replace("÷", "/")
            .replace("plus", "+")
            .replace("minus", "-")
            .replace("to the power of", "^")
            .replace("to the power", "^")
            .replace("squared", "^2")
            .replace("cubed", "^3")
            .replace("point", ".")
            .replace(Regex("\\s+"), "")
            .trim()
    }

    private fun wordsToNumbers(input: String): String {
        val ones = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3",
            "four" to "4", "five" to "5", "six" to "6", "seven" to "7",
            "eight" to "8", "nine" to "9", "ten" to "10", "eleven" to "11",
            "twelve" to "12", "thirteen" to "13", "fourteen" to "14",
            "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
            "eighteen" to "18", "nineteen" to "19"
        )
        val tens = mapOf(
            "twenty" to "20", "thirty" to "30", "forty" to "40",
            "fifty" to "50", "sixty" to "60", "seventy" to "70",
            "eighty" to "80", "ninety" to "90"
        )

        var result = input
        for ((tensWord, tensVal) in tens) {
            for ((onesWord, onesVal) in ones) {
                result = result.replace("$tensWord $onesWord", (tensVal.toInt() + onesVal.toInt()).toString())
            }
            result = result.replace(tensWord, tensVal)
        }
        for ((word, num) in ones) {
            result = result.replace(Regex("\\b$word\\b"), num)
        }
        result = result.replace(Regex("(\\d+)\\s*hundred")) { mr ->
            (mr.groupValues[1].toInt() * 100).toString()
        }
        return result
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_result")
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.destroy()
        tts?.shutdown()
    }
}