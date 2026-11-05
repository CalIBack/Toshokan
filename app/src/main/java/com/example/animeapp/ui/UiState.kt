package com.example.animeapp.ui

// Estado de una pantalla: cargando, con datos o con error
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
