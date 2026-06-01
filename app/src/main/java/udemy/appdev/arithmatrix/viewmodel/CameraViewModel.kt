package udemy.appdev.arithmatrix.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import udemy.appdev.arithmatrix.data.local.HistoryEntity
import udemy.appdev.arithmatrix.data.repository.HistoryRepository
import udemy.appdev.arithmatrix.engine.CalculatorEngine
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val calculatorEngine: CalculatorEngine
) : ViewModel() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> get() = _recognizedText

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> get() = _result

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> get() = _isProcessing

    fun processImage(bitmap: Bitmap) {
        _isProcessing.value = true
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extracted = visionText.text.replace("\\s".toRegex(), "")
                _recognizedText.value = extracted

                val res = try {
                    val raw = calculatorEngine.evaluate(extracted)
                    calculatorEngine.formatResult(raw)
                } catch (e: Exception) {
                    "Error"
                }
                _result.value = res

                if (res != "Error") {
                    viewModelScope.launch {
                        historyRepository.insert(
                            HistoryEntity(
                                expression = extracted,
                                result = res,
                                source = "CAMERA",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                _isProcessing.value = false
            }
            .addOnFailureListener {
                _recognizedText.value = ""
                _result.value = "Error"
                _isProcessing.value = false
            }
    }

    fun clearAll() {
        _result.value = ""
        _recognizedText.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
    }
}