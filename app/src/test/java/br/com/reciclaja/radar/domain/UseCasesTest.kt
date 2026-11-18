package br.com.reciclaja.radar.domain

import br.com.reciclaja.radar.domain.engine.AlertEngine
import br.com.reciclaja.radar.domain.model.Alert
import br.com.reciclaja.radar.domain.model.AlertLevel
import br.com.reciclaja.radar.domain.model.DAY_MS
import br.com.reciclaja.radar.domain.model.DataOrigin
import br.com.reciclaja.radar.domain.model.Decision
import br.com.reciclaja.radar.domain.model.DecisionAction
import br.com.reciclaja.radar.domain.model.Indicator
import br.com.reciclaja.radar.domain.model.Justification
import br.com.reciclaja.radar.domain.model.Reading
import br.com.reciclaja.radar.domain.model.Territory
import br.com.reciclaja.radar.domain.model.TerritoryType
import br.com.reciclaja.radar.domain.model.Theme
import br.com.reciclaja.radar.domain.usecase.AlertNotifier
import br.com.reciclaja.radar.domain.usecase.BuildTerritoryOverviewUseCase
import br.com.reciclaja.radar.domain.usecase.NotifiedAlertsStore
import br.com.reciclaja.radar.domain.usecase.NotifyCriticalAlertsUseCase
import br.com.reciclaja.radar.domain.usecase.RecordDecisionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fake do repositório: os casos de uso são testados sem Room e sem Android. */
private class FakeRepo : RadarRepository {
    val saved = mutableListOf<Decision>()
    override fun territories(): Flow<List<Territory>> = emptyFlow()
    override fun indicators(): Flow<List<Indicator>> = emptyFlow()
    override fun readings(territoryId: String): Flow<List<Reading>> = emptyFlow()
    override fun decisions(): Flow<List<Decision>> = emptyFlow()
    override suspend fun saveDecision(decision: Decision): Long { saved += decision; return saved.size.toLong() }
    override suspend fun lastLoadAt(): Long? = null
    override suspend fun updateThresholds(indicatorId: String, attention: Double, critical: Double) = Unit
    override suspend fun clearDecisions() = Unit
    override fun sourceInfo(): SourceInfo? = null
    override suspend fun ensureSeeded() = Unit
    override suspend fun reloadSyntheticData() = Unit
}

private fun alert(level: AlertLevel, day: Long = 100) = Alert(
    "t1", "ocupacao", level, "titulo", "resumo", 0.8, 95.0, 3,
    Justification(emptyList(), "regra", 0, day * DAY_MS, "Base Sintética", day * DAY_MS),
)

class UseCasesTest {
    @Test fun `decisao valida e persistida com responsavel e data`() = runTest {
        val repo = FakeRepo()
        val r = RecordDecisionUseCase(repo) { 42L }(alert(AlertLevel.CRITICO), DecisionAction.ACATAR, "  Ana Souza ", "")
        assertTrue(r is RecordDecisionUseCase.Result.Saved)
        assertEquals("Ana Souza", repo.saved.single().responsible)
        assertEquals(42L, repo.saved.single().createdAt)
    }

    @Test fun `decisao sem responsavel e rejeitada e nada e salvo`() = runTest {
        val repo = FakeRepo()
        val r = RecordDecisionUseCase(repo) { 0L }(alert(AlertLevel.CRITICO), DecisionAction.ACATAR, " ", "")
        assertTrue(r is RecordDecisionUseCase.Result.Invalid)
        assertTrue(repo.saved.isEmpty())
    }

    @Test fun `descartar exige motivo na observacao`() = runTest {
        val repo = FakeRepo()
        val r = RecordDecisionUseCase(repo) { 0L }(alert(AlertLevel.ATENCAO), DecisionAction.DESCARTAR, "Ana", "")
        assertTrue(r is RecordDecisionUseCase.Result.Invalid)
    }

    private class FakeNotifier(val allowed: Boolean) : AlertNotifier {
        val sent = mutableListOf<Alert>()
        override fun canNotify() = allowed
        override fun notifyCritical(alert: Alert, territoryName: String) { sent += alert }
    }

    private class MemoryStore : NotifiedAlertsStore {
        val keys = mutableSetOf<String>()
        override fun wasNotified(key: String) = key in keys
        override fun markNotified(key: String) { keys += key }
    }

    @Test fun `notifica so alerta critico e uma unica vez`() {
        val n = FakeNotifier(true)
        val uc = NotifyCriticalAlertsUseCase(n, MemoryStore())
        val alerts = listOf(alert(AlertLevel.CRITICO), alert(AlertLevel.ATENCAO))
        assertEquals(1, uc(alerts, "Jardim"))
        assertEquals(0, uc(alerts, "Jardim")) // mesma chave: não duplica
        assertEquals(1, n.sent.size)
    }

    @Test fun `sem permissao nao notifica e nao marca como notificado`() {
        val store = MemoryStore()
        assertEquals(0, NotifyCriticalAlertsUseCase(FakeNotifier(false), store)(listOf(alert(AlertLevel.CRITICO)), "J"))
        assertTrue(store.keys.isEmpty()) // quando a permissão vier, o alerta ainda será notificado
    }

    @Test fun `overview deriva alertas ordenados por gravidade e anexa contexto de chuva`() {
        val now = 1_800_000_000_000L
        val occ = Indicator("ocupacao", Theme.RESIDUOS, "Ocupação", "%", 75.0, 90.0, "")
        val rain = Indicator("chuva", Theme.CLIMA, "Chuva", "mm/24h", 30.0, 50.0, "")
        val readings = (0 until 14).flatMap { d ->
            val t = now - (13 - d) * DAY_MS
            listOf(
                Reading("ocupacao", "t1", t, 70.0 + 1.5 * d, DataOrigin.SINTETICA),
                Reading("chuva", "t1", t, 31.0, DataOrigin.SINTETICA),
            )
        }
        val ov = BuildTerritoryOverviewUseCase(AlertEngine())(Territory("t1", "T", TerritoryType.BAIRRO, 0.0, 0.0), listOf(rain, occ), readings, now)
        assertEquals(listOf(AlertLevel.CRITICO, AlertLevel.ATENCAO), ov.alerts.map { it.level })
        assertTrue(ov.alerts.first().justification.variables.any { it.first.startsWith("Chuva") })
    }
}
