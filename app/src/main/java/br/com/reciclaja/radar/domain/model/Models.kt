package br.com.reciclaja.radar.domain.model

// Domínio em Kotlin puro: nenhuma importação de Android, Room ou Compose.

enum class TerritoryType(val label: String) { CIDADE("Cidade"), BAIRRO("Bairro"), AREA_RURAL("Área rural") }

enum class Theme(val label: String) { RESIDUOS("Resíduos"), CLIMA("Clima") }

enum class DataOrigin(val label: String) { PUBLICA("Pública"), SINTETICA("Sintética"), AUTORIZADA("Autorizada") }

data class Territory(
    val id: String,
    val name: String,
    val type: TerritoryType,
    val lat: Double,
    val lon: Double,
)

data class Indicator(
    val id: String,
    val theme: Theme,
    val name: String,
    val unit: String,
    val attentionThreshold: Double,
    val criticalThreshold: Double,
    val description: String,
)

data class Reading(
    val indicatorId: String,
    val territoryId: String,
    val timestamp: Long,
    val value: Double,
    val origin: DataOrigin,
)

enum class AlertLevel(val label: String) { INFORMATIVO("Informativo"), ATENCAO("Atenção"), CRITICO("Crítico") }

data class Justification(
    val variables: List<Pair<String, String>>, // nome -> valor formatado
    val rule: String,
    val periodStart: Long,
    val periodEnd: Long,
    val source: String,
    val updatedAt: Long,
)

data class Alert(
    val territoryId: String,
    val indicatorId: String,
    val level: AlertLevel,
    val title: String,
    val summary: String,
    val confidence: Double, // 0..1
    val forecastValue: Double,
    val forecastHorizonDays: Int,
    val justification: Justification,
) {
    /** Chave estável do alerta: um alerta por território+indicador+nível+dia da última leitura. */
    val key: String get() = "$territoryId|$indicatorId|${level.name}|${justification.periodEnd / DAY_MS}"
    val confidenceLabel: String
        get() = when {
            confidence >= 0.75 -> "Alta"
            confidence >= 0.5 -> "Média"
            else -> "Baixa"
        }
}

enum class DecisionAction(val label: String) { ACATAR("Acatar"), ADIAR("Adiar"), DESCARTAR("Descartar") }

data class Decision(
    val id: Long = 0,
    val alertKey: String,
    val territoryId: String,
    val indicatorId: String,
    val alertLevel: AlertLevel,
    val alertTitle: String,
    val action: DecisionAction,
    val responsible: String,
    val note: String,
    val createdAt: Long,
)

/** Resultado da avaliação de um indicador: ou há alerta, ou há um motivo explícito para não haver. */
sealed interface Evaluation {
    data class Raised(val alert: Alert) : Evaluation
    data object Normal : Evaluation
    data class InsufficientData(val available: Int, val required: Int) : Evaluation
    data class StaleData(val lastReadingAt: Long) : Evaluation
    data object NoData : Evaluation
}

const val DAY_MS = 24L * 60 * 60 * 1000
