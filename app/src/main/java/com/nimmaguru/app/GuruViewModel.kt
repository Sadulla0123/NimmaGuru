package com.nimmaguru.app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class GuruViewModel(
    private val repository: GuruRepository = GuruRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(GuruUiState())
    val uiState: StateFlow<GuruUiState> = _uiState

    init {
        repository.connect(
            onGurusChanged = { gurus -> _uiState.update { it.copy(gurus = gurus, isLoading = false) } },
            onClassesChanged = { classes -> _uiState.update { it.copy(classes = classes) } },
            onStudentActionsChanged = { actions -> _uiState.update { it.copy(studentActions = actions) } },
            onError = { message -> _uiState.update { it.copy(errorMessage = message, isLoading = false) } }
        )
    }

    fun selectSkill(skill: String?) {
        _uiState.update { it.copy(selectedSkill = skill) }
    }

    fun updateLocality(query: String) {
        _uiState.update { it.copy(localityQuery = query) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun postAppreciation(guru: Guru, studentName: String, message: String) {
        repository.postAppreciation(guru, studentName, message) { error ->
            _uiState.update { it.copy(errorMessage = error) }
        }
    }

    fun requestSession(guru: Guru, studentName: String) {
        repository.requestSession(guru, studentName) { error ->
            _uiState.update { it.copy(errorMessage = error) }
        }
    }

    fun saveProfile(guru: Guru, skills: List<String>, freeHours: String, contact: String) {
        repository.updateGuru(guru, skills, freeHours, contact) { error ->
            _uiState.update { it.copy(errorMessage = error) }
        }
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}
