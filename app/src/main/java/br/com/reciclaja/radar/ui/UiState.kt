package br.com.reciclaja.radar.ui

/** Estados de tela padronizados (renderizados por StateView). */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    /** Dados existem mas estão velhos: mostra com aviso e sem emitir alerta novo. */
    data class Stale<T>(val data: T, val lastUpdate: Long) : UiState<T>
    data class Empty(val message: String) : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}
