package edu.nd.pmcburne.hwapp.one.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import edu.nd.pmcburne.hwapp.one.data.BasketballRepository
import edu.nd.pmcburne.hwapp.one.data.local.GameEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScoreUiState(
    val selectedDate: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    val selectedGender: String = "men",
    val games: List<GameEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasAttemptedRefresh: Boolean = false,
    val lastUpdatedText: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ScoreViewModel(
    private val repository: BasketballRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now(ZoneId.systemDefault()))
    private val selectedGender = MutableStateFlow("men")
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val attemptedRefresh = MutableStateFlow(false)
    private val lastUpdatedText = MutableStateFlow<String?>(null)

    private val gamesFlow = combine(selectedDate, selectedGender) { date, gender ->
        Pair(date, gender)
    }.flatMapLatest { (date, gender) ->
        repository.observeGames(gender, date)
    }

    private val selectionFlow = combine(selectedDate, selectedGender) { date, gender ->
        Pair(date, gender)
    }

    private val statusFlow = combine(
        loading,
        error,
        attemptedRefresh,
        lastUpdatedText
    ) { isLoading, errorMessage, hasAttempted, updatedText ->
        StatusBundle(
            isLoading = isLoading,
            errorMessage = errorMessage,
            hasAttemptedRefresh = hasAttempted,
            lastUpdatedText = updatedText
        )
    }

    val uiState: StateFlow<ScoreUiState> = combine(
        selectionFlow,
        statusFlow,
        gamesFlow
    ) { selection, status, games ->
        ScoreUiState(
            selectedDate = selection.first,
            selectedGender = selection.second,
            games = games,
            isLoading = status.isLoading,
            errorMessage = status.errorMessage,
            hasAttemptedRefresh = status.hasAttemptedRefresh,
            lastUpdatedText = status.lastUpdatedText
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScoreUiState()
    )

    init {
        refresh()
    }

    fun onDateSelected(newDate: LocalDate) {
        selectedDate.value = newDate
        refresh()
    }

    fun onGenderSelected(newGender: String) {
        selectedGender.value = newGender
        refresh()
    }
    fun goToPreviousDay() {
        selectedDate.value = selectedDate.value.minusDays(1)
        refresh()
    }

    fun goToNextDay() {
        selectedDate.value = selectedDate.value.plusDays(1)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            attemptedRefresh.value = true
            try {
                repository.refreshGames(
                    gender = selectedGender.value,
                    date = selectedDate.value
                )
                lastUpdatedText.value = LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("h:mm a"))
            } catch (_: Exception) {
                error.value = "Showing saved data. Could not update from the internet."
            } finally {
                loading.value = false
            }
        }
    }

    companion object {
        fun factory(repository: BasketballRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ScoreViewModel(repository) as T
                }
            }
    }
}

private data class StatusBundle(
    val isLoading: Boolean,
    val errorMessage: String?,
    val hasAttemptedRefresh: Boolean,
    val lastUpdatedText: String?
)