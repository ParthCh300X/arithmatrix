package udemy.appdev.arithmatrix.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import udemy.appdev.arithmatrix.data.local.HistoryEntity
import udemy.appdev.arithmatrix.data.repository.HistoryRepository
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository
) : ViewModel() {

    val history: StateFlow<List<HistoryEntity>> =
        repository.getAllHistory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    // mode param is now actually used when building the entity
    fun insert(expression: String, result: String, mode: String = "BASIC") {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.insert(
                    HistoryEntity(
                        expression = expression,
                        result = result,
                        source = mode,
                        timestamp = System.currentTimeMillis()
                    )
                )
                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun delete(entry: HistoryEntity) {
        viewModelScope.launch {
            try {
                repository.delete(entry)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Delete failed")
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                repository.clearAll()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Clear failed")
            }
        }
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Error(val message: String) : UiState()
}