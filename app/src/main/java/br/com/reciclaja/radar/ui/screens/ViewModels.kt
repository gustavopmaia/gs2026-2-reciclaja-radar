package br.com.reciclaja.radar.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.reciclaja.radar.AppContainer
import br.com.reciclaja.radar.domain.SourceInfo
import br.com.reciclaja.radar.domain.model.Alert
import br.com.reciclaja.radar.domain.model.Decision
import br.com.reciclaja.radar.domain.model.DecisionAction
import br.com.reciclaja.radar.domain.model.Evaluation
import br.com.reciclaja.radar.domain.model.Indicator
import br.com.reciclaja.radar.domain.model.Territory
import br.com.reciclaja.radar.domain.model.Theme
import br.com.reciclaja.radar.domain.usecase.IndicatorStatus
import br.com.reciclaja.radar.domain.usecase.RecordDecisionUseCase
import br.com.reciclaja.radar.domain.usecase.TerritoryOverview
import br.com.reciclaja.radar.ui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val WHILE_SUBSCRIBED = SharingStarted.WhileSubscribed(5_000)

/** Fluxo compartilhado: território -> leituras do Room -> estado derivado (indicadores + alertas). */
@OptIn(ExperimentalCoroutinesApi::class)
private fun AppContainer.overviewFlow(territoryId: Flow<String?>, retry: Flow<Int>): Flow<UiState<TerritoryOverview>> =
    combine(repository.territories(), repository.indicators(), prefs.showClimate, territoryId, retry) { t, i, climate, sel, _ ->
        Triple(t, i.filter { climate || it.theme != Theme.CLIMA }, sel)
    }.flatMapLatest { (territories, indicators, sel) ->
        if (territories.isEmpty()) return@flatMapLatest flowOf<UiState<TerritoryOverview>>(UiState.Loading)
        val territory = territories.firstOrNull { it.id == sel } ?: territories.first()
        repository.readings(territory.id).map { readings ->
            val ov = buildOverview(territory, indicators, readings, clock())
            when {
                ov.hasNoData -> UiState.Empty("Ainda não há leituras para ${territory.name}. Nenhum alerta é emitido sem dados. Escolha outro território ou recarregue a base em Configurações.")
                ov.isStale -> UiState.Stale(ov, ov.lastReadingAt ?: 0L)
                else -> UiState.Success(ov)
            }
        }
    }.catch { emit(UiState.Error("Não foi possível ler os dados locais (${it.message ?: "falha desconhecida"}).")) }

class DashboardViewModel(private val c: AppContainer) : ViewModel() {
    private val retry = MutableStateFlow(0)
    val territories: StateFlow<List<Territory>> = c.repository.territories().stateIn(viewModelScope, WHILE_SUBSCRIBED, emptyList())
    val selectedId: StateFlow<String?> = c.prefs.selectedTerritory
    val state: StateFlow<UiState<TerritoryOverview>> = c.overviewFlow(c.prefs.selectedTerritory, retry)
        .onEach { s -> (s as? UiState.Success)?.data?.let { notify(it) } }
        .stateIn(viewModelScope, WHILE_SUBSCRIBED, UiState.Loading)
    private val _lastLoad = MutableStateFlow<Long?>(null)
    val lastLoad: StateFlow<Long?> = _lastLoad.asStateFlow()

    init { viewModelScope.launch { c.repository.ensureSeeded(); _lastLoad.value = c.repository.lastLoadAt() } }

    fun select(id: String) = c.prefs.selectTerritory(id)
    fun retry() { retry.value++ }
    fun now() = c.clock()

    /** Chamado também quando a permissão passa a ser concedida. */
    fun onPermissionMaybeChanged() { (state.value as? UiState.Success)?.data?.let { notify(it) } }
    fun canNotify() = c.notifier.canNotify()

    private fun notify(ov: TerritoryOverview) = c.notifyCritical(ov.alerts, ov.territory.name)
}

/** Usado por Detalhe do Indicador e Detalhe do Alerta: recebe só IDs pela rota. */
class DetailViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val territoryId: String = checkNotNull(handle["territoryId"])
    val indicatorId: String = checkNotNull(handle["indicatorId"])
    private val retry = MutableStateFlow(0)

    val state: StateFlow<UiState<TerritoryOverview>> = c.overviewFlow(flowOf(territoryId), retry)
        .stateIn(viewModelScope, WHILE_SUBSCRIBED, UiState.Loading)

    val decisions: StateFlow<List<Decision>> = c.repository.decisions()
        .map { all -> all.filter { it.territoryId == territoryId && it.indicatorId == indicatorId } }
        .stateIn(viewModelScope, WHILE_SUBSCRIBED, emptyList())

    private val _decisionError = MutableStateFlow<String?>(null)
    val decisionError: StateFlow<String?> = _decisionError.asStateFlow()
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun status(ov: TerritoryOverview): IndicatorStatus? = ov.items.firstOrNull { it.indicator.id == indicatorId }
    fun alert(ov: TerritoryOverview): Alert? = (status(ov)?.evaluation as? Evaluation.Raised)?.alert
    fun retry() { retry.value++ }
    fun now() = c.clock()

    fun decide(alert: Alert, action: DecisionAction, responsible: String, note: String) = viewModelScope.launch {
        when (val r = c.recordDecision(alert, action, responsible, note)) {
            is RecordDecisionUseCase.Result.Invalid -> _decisionError.value = r.reason
            is RecordDecisionUseCase.Result.Saved -> { _decisionError.value = null; _saved.value = true }
        }
    }
    fun consumeSaved() { _saved.value = false }
    fun clearError() { _decisionError.value = null }
}

data class HistoryRow(val decision: Decision, val territoryName: String, val indicatorName: String)

class HistoryViewModel(c: AppContainer) : ViewModel() {
    val state: StateFlow<UiState<List<HistoryRow>>> =
        combine(c.repository.decisions(), c.repository.territories(), c.repository.indicators()) { d, t, i ->
            val tn = t.associate { it.id to it.name }
            val inn = i.associate { it.id to it.name }
            if (d.isEmpty()) UiState.Empty("Nenhuma decisão registrada ainda. Abra um alerta e registre Acatar, Adiar ou Descartar.")
            else UiState.Success(d.map { HistoryRow(it, tn[it.territoryId] ?: it.territoryId, inn[it.indicatorId] ?: it.indicatorId) })
        }.catch { emit(UiState.Error("Falha ao ler o histórico: ${it.message}")) }
            .stateIn(viewModelScope, WHILE_SUBSCRIBED, UiState.Loading)
}

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val territories = c.repository.territories().stateIn(viewModelScope, WHILE_SUBSCRIBED, emptyList())
    val indicators: StateFlow<List<Indicator>> = c.repository.indicators().stateIn(viewModelScope, WHILE_SUBSCRIBED, emptyList())
    val selectedId = c.prefs.selectedTerritory
    val showClimate = c.prefs.showClimate
    private val _info = MutableStateFlow<Pair<SourceInfo?, Long?>>(null to null)
    val info: StateFlow<Pair<SourceInfo?, Long?>> = _info.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init { refreshInfo() }

    private fun refreshInfo() = viewModelScope.launch {
        c.repository.ensureSeeded()
        _info.value = c.repository.sourceInfo() to c.repository.lastLoadAt()
    }

    fun select(id: String) = c.prefs.selectTerritory(id)
    fun setShowClimate(v: Boolean) = c.prefs.setShowClimate(v)
    fun saveThresholds(id: String, attention: String, critical: String) = viewModelScope.launch {
        val a = attention.replace(',', '.').toDoubleOrNull()
        val cr = critical.replace(',', '.').toDoubleOrNull()
        _message.value = if (a == null || cr == null || a <= 0 || cr <= a) "Limiares inválidos: use números com atenção < crítico."
        else { c.repository.updateThresholds(id, a, cr); "Limiares salvos." }
    }
    fun reload() = viewModelScope.launch { c.repository.reloadSyntheticData(); refreshInfo(); _message.value = "Base sintética recarregada." }
    fun clearHistory() = viewModelScope.launch { c.repository.clearDecisions(); _message.value = "Histórico de decisões apagado." }
    fun consumeMessage() { _message.value = null }
    fun permissionAsked() = c.prefs.notificationPermissionAsked
    fun markPermissionAsked() { c.prefs.notificationPermissionAsked = true }
    fun hasNotificationPermission() = c.notifier.hasPermission()
}

fun factory(c: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { DashboardViewModel(c) }
    initializer { DetailViewModel(c, createSavedStateHandle()) }
    initializer { HistoryViewModel(c) }
    initializer { SettingsViewModel(c) }
}
