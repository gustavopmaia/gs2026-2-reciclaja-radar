package br.com.reciclaja.radar.data

import android.content.Context
import br.com.reciclaja.radar.data.local.DecisionEntity
import br.com.reciclaja.radar.data.local.RadarDao
import br.com.reciclaja.radar.data.prefs.AppPreferences
import br.com.reciclaja.radar.domain.RadarRepository
import br.com.reciclaja.radar.domain.SourceInfo
import br.com.reciclaja.radar.domain.model.AlertLevel
import br.com.reciclaja.radar.domain.model.DataOrigin
import br.com.reciclaja.radar.domain.model.Decision
import br.com.reciclaja.radar.domain.model.DecisionAction
import br.com.reciclaja.radar.domain.model.Indicator
import br.com.reciclaja.radar.domain.model.Reading
import br.com.reciclaja.radar.domain.model.Territory
import br.com.reciclaja.radar.domain.model.TerritoryType
import br.com.reciclaja.radar.domain.model.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Implementação offline-first: Room é a fonte única; o seed sintético entra uma vez (ou sob demanda). */
class RoomRadarRepository(
    private val context: Context,
    private val dao: RadarDao,
    private val prefs: AppPreferences,
    private val clock: () -> Long,
) : RadarRepository {
    private val seedLock = Mutex()
    @Volatile private var info: SourceInfo? = null

    override fun territories(): Flow<List<Territory>> = dao.territories().map { list ->
        list.map { Territory(it.id, it.name, TerritoryType.valueOf(it.type), it.lat, it.lon) }
    }

    override fun indicators(): Flow<List<Indicator>> = dao.indicators().map { list ->
        list.map { Indicator(it.id, Theme.valueOf(it.theme), it.name, it.unit, it.attention, it.critical, it.description) }
    }

    override fun readings(territoryId: String): Flow<List<Reading>> = dao.readings(territoryId).map { list ->
        list.map { Reading(it.indicatorId, it.territoryId, it.timestamp, it.value, DataOrigin.valueOf(it.origin)) }
    }

    override fun decisions(): Flow<List<Decision>> = dao.decisions().map { list ->
        list.map {
            Decision(it.id, it.alertKey, it.territoryId, it.indicatorId, AlertLevel.valueOf(it.alertLevel), it.alertTitle,
                DecisionAction.valueOf(it.action), it.responsible, it.note, it.createdAt)
        }
    }

    override suspend fun saveDecision(decision: Decision): Long = dao.insertDecision(
        DecisionEntity(
            alertKey = decision.alertKey, territoryId = decision.territoryId, indicatorId = decision.indicatorId,
            alertLevel = decision.alertLevel.name, alertTitle = decision.alertTitle, action = decision.action.name,
            responsible = decision.responsible, note = decision.note, createdAt = decision.createdAt,
        )
    )

    override suspend fun lastLoadAt(): Long? = prefs.lastLoadAt

    override suspend fun updateThresholds(indicatorId: String, attention: Double, critical: Double) =
        dao.updateThresholds(indicatorId, attention, critical)

    override suspend fun clearDecisions() = dao.clearDecisions()

    override fun sourceInfo(): SourceInfo? = info

    override suspend fun ensureSeeded() = seedLock.withLock {
        if (dao.territoryCount() == 0) load() else if (info == null) info = readSeed(prefs.lastLoadAt ?: clock()).info
    }

    override suspend fun reloadSyntheticData() = seedLock.withLock { load() }

    private suspend fun load() {
        val now = clock()
        val seed = readSeed(now)
        dao.replaceSeed(seed.territories, seed.indicators, seed.readings)
        prefs.lastLoadAt = now
        info = seed.info
    }

    private suspend fun readSeed(anchor: Long) = withContext(Dispatchers.IO) {
        val json = context.assets.open("seed.json").bufferedReader().use { it.readText() }
        SeedParser.parse(json, anchor)
    }
}
