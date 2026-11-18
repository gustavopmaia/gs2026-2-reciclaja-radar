package br.com.reciclaja.radar.domain.engine

import br.com.reciclaja.radar.domain.model.Alert
import br.com.reciclaja.radar.domain.model.AlertLevel
import br.com.reciclaja.radar.domain.model.DAY_MS
import br.com.reciclaja.radar.domain.model.Evaluation
import br.com.reciclaja.radar.domain.model.Indicator
import br.com.reciclaja.radar.domain.model.Justification
import br.com.reciclaja.radar.domain.model.Reading
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Motor de previsão explicável (Kotlin puro, sem ML):
 *  - janela de [windowDays] dias; exige [minReadings] leituras e última leitura com menos de [maxAgeDays] dias;
 *  - média móvel de 7 dias (nível atual) + regressão linear (tendência) projetada [horizonDays] dias à frente;
 *  - nível pelo maior entre previsão e média móvel versus limiares do indicador;
 *  - confiança = R² da regressão × completude da janela.
 * ponytail: regressão linear simples; trocar por sazonalidade/modelo leve se o eval de falso positivo pedir.
 */
class AlertEngine(
    private val windowDays: Int = 14,
    private val minReadings: Int = 7,
    private val maxAgeDays: Int = 2,
    private val horizonDays: Int = 3,
) {
    data class Trend(val slopePerDay: Double, val intercept: Double, val r2: Double)

    fun evaluate(indicator: Indicator, readings: List<Reading>, now: Long, extraContext: List<Pair<String, String>> = emptyList()): Evaluation {
        if (readings.isEmpty()) return Evaluation.NoData
        val last = readings.maxOf { it.timestamp }
        if (now - last > maxAgeDays * DAY_MS) return Evaluation.StaleData(last)

        val windowStart = now - windowDays * DAY_MS
        val window = readings.filter { it.timestamp >= windowStart }.sortedBy { it.timestamp }
        if (window.size < minReadings) return Evaluation.InsufficientData(window.size, minReadings)

        val ma7 = movingAverage(window, 7)
        val trend = linearTrend(window)
        val daysAhead = (last - window.first().timestamp).toDouble() / DAY_MS + horizonDays
        val forecast = trend.intercept + trend.slopePerDay * daysAhead
        val completeness = min(1.0, window.size.toDouble() / windowDays)
        val confidence = (max(0.0, trend.r2) * completeness).coerceIn(0.0, 1.0)

        val reference = max(forecast, ma7)
        val level = when {
            reference >= indicator.criticalThreshold -> AlertLevel.CRITICO
            reference >= indicator.attentionThreshold -> AlertLevel.ATENCAO
            trend.slopePerDay > 0 && reference >= indicator.attentionThreshold * 0.9 -> AlertLevel.INFORMATIVO
            else -> return Evaluation.Normal
        }

        val weeklyChangePct = if (ma7 > 0) trend.slopePerDay * 7 / ma7 * 100 else 0.0
        val pctOfCritical = forecast / indicator.criticalThreshold * 100
        val u = indicator.unit
        val justification = Justification(
            variables = listOf(
                "Média móvel 7 dias" to "${fmt(ma7)} $u",
                "Tendência" to "${signed(trend.slopePerDay)} $u/dia (${signed(weeklyChangePct)}% por semana)",
                "Previsão em $horizonDays dias" to "${fmt(forecast)} $u (${fmt(pctOfCritical)}% do limiar crítico)",
                "Limiares (atenção / crítico)" to "${fmt(indicator.attentionThreshold)} / ${fmt(indicator.criticalThreshold)} $u",
                "Leituras na janela" to "${window.size} de $windowDays dias",
                "Ajuste da tendência (R²)" to fmt(trend.r2, 2),
            ) + extraContext,
            rule = "Nível = maior valor entre previsão linear em $horizonDays dias e média móvel de 7 dias, comparado aos limiares do indicador. " +
                "Crítico ≥ ${fmt(indicator.criticalThreshold)}; Atenção ≥ ${fmt(indicator.attentionThreshold)}; " +
                "Informativo ≥ 90% do limiar de atenção com tendência de alta.",
            periodStart = window.first().timestamp,
            periodEnd = last,
            source = window.map { it.origin.label }.distinct().joinToString() .let { "Base $it" },
            updatedAt = last,
        )
        return Evaluation.Raised(
            Alert(
                territoryId = window.first().territoryId,
                indicatorId = indicator.id,
                level = level,
                title = "${level.label}: ${indicator.name.lowercase(Locale("pt", "BR"))} pode chegar a ${fmt(forecast)} $u",
                summary = "Previsão para os próximos $horizonDays dias, com tendência de ${signed(weeklyChangePct)}% por semana.",
                confidence = confidence,
                forecastValue = forecast,
                forecastHorizonDays = horizonDays,
                justification = justification,
            )
        )
    }

    fun movingAverage(sorted: List<Reading>, n: Int): Double = sorted.takeLast(n).map { it.value }.average()

    fun linearTrend(sorted: List<Reading>): Trend {
        val t0 = sorted.first().timestamp
        val xs = sorted.map { (it.timestamp - t0).toDouble() / DAY_MS }
        val ys = sorted.map { it.value }
        val mx = xs.average()
        val my = ys.average()
        val sxx = xs.sumOf { (it - mx) * (it - mx) }
        if (sxx == 0.0) return Trend(0.0, my, 0.0)
        val sxy = xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) }
        val slope = sxy / sxx
        val intercept = my - slope * mx
        val ssTot = ys.sumOf { (it - my) * (it - my) }
        val ssRes = xs.indices.sumOf { val e = ys[it] - (intercept + slope * xs[it]); e * e }
        val r2 = if (ssTot == 0.0) 1.0 else 1 - ssRes / ssTot
        return Trend(slope, intercept, r2)
    }

    /** Pontos previstos (dia relativo -> valor) para o gráfico, a partir da mesma regressão do alerta. */
    fun forecastPoints(readings: List<Reading>, now: Long): List<Pair<Long, Double>> {
        val window = readings.filter { it.timestamp >= now - windowDays * DAY_MS }.sortedBy { it.timestamp }
        if (window.size < minReadings) return emptyList()
        val trend = linearTrend(window)
        val t0 = window.first().timestamp
        val last = window.last().timestamp
        return (0..horizonDays).map { d ->
            val t = last + d * DAY_MS
            t to trend.intercept + trend.slopePerDay * ((t - t0).toDouble() / DAY_MS)
        }
    }

    private fun fmt(v: Double, decimals: Int = 1) = String.format(Locale("pt", "BR"), "%.${decimals}f", v)
    private fun signed(v: Double) = (if (v >= 0) "+" else "") + fmt(v)
}
