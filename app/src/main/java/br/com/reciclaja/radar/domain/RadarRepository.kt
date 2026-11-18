package br.com.reciclaja.radar.domain

import br.com.reciclaja.radar.domain.model.Decision
import br.com.reciclaja.radar.domain.model.Indicator
import br.com.reciclaja.radar.domain.model.Reading
import br.com.reciclaja.radar.domain.model.Territory
import kotlinx.coroutines.flow.Flow

/** Contrato entre domínio e dados. Trocar Room/fonte sintética por API pública não altera o domínio. */
interface RadarRepository {
    fun territories(): Flow<List<Territory>>
    fun indicators(): Flow<List<Indicator>>
    fun readings(territoryId: String): Flow<List<Reading>>
    fun decisions(): Flow<List<Decision>>
    suspend fun saveDecision(decision: Decision): Long
    suspend fun lastLoadAt(): Long?
    suspend fun updateThresholds(indicatorId: String, attention: Double, critical: Double)
    suspend fun clearDecisions()
    fun sourceInfo(): SourceInfo?
    /** Garante a carga inicial (idempotente). */
    suspend fun ensureSeeded()
    /** Recarrega a base sintética reancorada na data atual. */
    suspend fun reloadSyntheticData()
}

/** Metadados da fonte de dados exibidos em Configurações (origem e licença). */
data class SourceInfo(val source: String, val license: String, val origin: String, val version: String, val note: String)
