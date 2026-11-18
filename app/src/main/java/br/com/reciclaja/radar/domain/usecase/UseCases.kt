package br.com.reciclaja.radar.domain.usecase

import br.com.reciclaja.radar.domain.RadarRepository
import br.com.reciclaja.radar.domain.engine.AlertEngine
import br.com.reciclaja.radar.domain.model.Alert
import br.com.reciclaja.radar.domain.model.AlertLevel
import br.com.reciclaja.radar.domain.model.DAY_MS
import br.com.reciclaja.radar.domain.model.Decision
import br.com.reciclaja.radar.domain.model.DecisionAction
import br.com.reciclaja.radar.domain.model.Evaluation
import br.com.reciclaja.radar.domain.model.Indicator
import br.com.reciclaja.radar.domain.model.Reading
import br.com.reciclaja.radar.domain.model.Territory
import java.util.Locale

/** Estado derivado de um indicador num território: nada aqui é persistido, tudo sai das leituras. */
data class IndicatorStatus(
    val indicator: Indicator,
    val history: List<Reading>,
    val latest: Reading?,
    val weeklyVariationPct: Double?,
    val evaluation: Evaluation,
    val forecast: List<Pair<Long, Double>>,
)

data class TerritoryOverview(
    val territory: Territory,
    val items: List<IndicatorStatus>,
    val lastReadingAt: Long?,
) {
    val alerts: List<Alert> = items.mapNotNull { (it.evaluation as? Evaluation.Raised)?.alert }
        .sortedByDescending { it.level.ordinal }
    val hasNoData: Boolean get() = items.all { it.history.isEmpty() }
    val isStale: Boolean get() = items.any { it.evaluation is Evaluation.StaleData }
}

/** Monta indicadores, gráficos e alertas de um território a partir das leituras (estado derivado). */
class BuildTerritoryOverviewUseCase(private val engine: AlertEngine) {
    operator fun invoke(territory: Territory, indicators: List<Indicator>, readings: List<Reading>, now: Long): TerritoryOverview {
        val byIndicator = readings.groupBy { it.indicatorId }
        val rain = byIndicator["chuva"].orEmpty().sortedBy { it.timestamp }.takeLast(3)
        val rainContext = if (rain.isNotEmpty())
            listOf("Chuva média (últimos 3 registros)" to String.format(Locale("pt", "BR"), "%.1f mm/24h", rain.map { it.value }.average()))
        else emptyList()

        val items = indicators.map { ind ->
            val hist = byIndicator[ind.id].orEmpty().sortedBy { it.timestamp }
            val latest = hist.lastOrNull()
            val weekAgo = latest?.let { l -> hist.lastOrNull { it.timestamp <= l.timestamp - 7 * DAY_MS } }
            val variation = if (latest != null && weekAgo != null && weekAgo.value != 0.0)
                (latest.value - weekAgo.value) / weekAgo.value * 100 else null
            val extra = if (ind.id != "chuva") rainContext else emptyList()
            IndicatorStatus(
                indicator = ind,
                history = hist,
                latest = latest,
                weeklyVariationPct = variation,
                evaluation = engine.evaluate(ind, hist, now, extra),
                forecast = engine.forecastPoints(hist, now),
            )
        }
        return TerritoryOverview(territory, items, readings.maxOfOrNull { it.timestamp })
    }
}

/** Registro da decisão humana: o app só recomenda, quem decide é o gestor. */
class RecordDecisionUseCase(private val repository: RadarRepository, private val clock: () -> Long) {
    sealed interface Result {
        data class Saved(val id: Long) : Result
        data class Invalid(val reason: String) : Result
    }

    suspend operator fun invoke(alert: Alert, action: DecisionAction, responsible: String, note: String): Result {
        val who = responsible.trim()
        val obs = note.trim()
        if (who.length < 3) return Result.Invalid("Informe o nome do responsável (mínimo 3 letras).")
        if (action != DecisionAction.ACATAR && obs.length < 5)
            return Result.Invalid("Para ${action.label.lowercase()} é obrigatório registrar o motivo na observação.")
        if (obs.length > 500) return Result.Invalid("Observação com no máximo 500 caracteres.")
        val id = repository.saveDecision(
            Decision(
                alertKey = alert.key,
                territoryId = alert.territoryId,
                indicatorId = alert.indicatorId,
                alertLevel = alert.level,
                alertTitle = alert.title,
                action = action,
                responsible = who,
                note = obs,
                createdAt = clock(),
            )
        )
        return Result.Saved(id)
    }
}

/** Porta para o recurso nativo de notificação (implementada em platform/). */
interface AlertNotifier {
    fun canNotify(): Boolean
    fun notifyCritical(alert: Alert, territoryName: String)
}

/** Porta para lembrar quais alertas já foram notificados (evita duplicar). */
interface NotifiedAlertsStore {
    fun wasNotified(key: String): Boolean
    fun markNotified(key: String)
}

/** Notifica apenas alertas CRÍTICOS, uma única vez por alerta, e só se a permissão existir. */
class NotifyCriticalAlertsUseCase(private val notifier: AlertNotifier, private val store: NotifiedAlertsStore) {
    /** @return quantidade de notificações efetivamente enviadas. */
    operator fun invoke(alerts: List<Alert>, territoryName: String): Int {
        if (!notifier.canNotify()) return 0
        var sent = 0
        alerts.filter { it.level == AlertLevel.CRITICO && !store.wasNotified(it.key) }.forEach {
            notifier.notifyCritical(it, territoryName)
            store.markNotified(it.key)
            sent++
        }
        return sent
    }
}
