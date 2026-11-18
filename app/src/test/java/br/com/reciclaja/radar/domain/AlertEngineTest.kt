package br.com.reciclaja.radar.domain

import br.com.reciclaja.radar.domain.engine.AlertEngine
import br.com.reciclaja.radar.domain.model.AlertLevel
import br.com.reciclaja.radar.domain.model.DAY_MS
import br.com.reciclaja.radar.domain.model.DataOrigin
import br.com.reciclaja.radar.domain.model.Evaluation
import br.com.reciclaja.radar.domain.model.Indicator
import br.com.reciclaja.radar.domain.model.Reading
import br.com.reciclaja.radar.domain.model.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEngineTest {
    private val engine = AlertEngine()
    private val now = 1_800_000_000_000L
    private val ocupacao = Indicator("ocupacao", Theme.RESIDUOS, "Ocupação dos contêineres", "%", 75.0, 90.0, "")

    /** Série diária terminando hoje: valor = start + slope*dia. */
    private fun series(days: Int, start: Double, slope: Double, endOffsetDays: Int = 0, skip: Set<Int> = emptySet()) =
        (0 until days).filter { it !in skip }.map { d ->
            Reading("ocupacao", "t1", now - (days - 1 - d + endOffsetDays) * DAY_MS, start + slope * d, DataOrigin.SINTETICA)
        }

    @Test fun `tendencia de alta acima do limiar gera alerta critico com justificativa completa`() {
        val e = engine.evaluate(ocupacao, series(14, 70.0, 1.5), now)
        assertTrue(e is Evaluation.Raised)
        val a = (e as Evaluation.Raised).alert
        assertEquals(AlertLevel.CRITICO, a.level)
        // previsão linear exata: 70 + 1.5 * (13 + 3) = 94
        assertEquals(94.0, a.forecastValue, 0.01)
        val names = a.justification.variables.map { it.first }
        assertTrue(names.contains("Média móvel 7 dias"))
        assertTrue(names.contains("Previsão em 3 dias"))
        assertTrue(a.justification.rule.contains("limiares"))
        assertTrue(a.justification.source.contains("Sintética"))
        assertTrue(a.confidence > 0.9) // série perfeita e completa
    }

    @Test fun `valor estavel entre atencao e critico gera alerta de atencao`() {
        val e = engine.evaluate(ocupacao, series(14, 80.0, 0.0), now) as Evaluation.Raised
        assertEquals(AlertLevel.ATENCAO, e.alert.level)
    }

    @Test fun `valores baixos e estaveis nao geram alerta`() {
        assertEquals(Evaluation.Normal, engine.evaluate(ocupacao, series(14, 40.0, 0.0), now))
    }

    @Test fun `sem leituras nao emite alerta`() {
        assertEquals(Evaluation.NoData, engine.evaluate(ocupacao, emptyList(), now))
    }

    @Test fun `dados desatualizados ha mais de 2 dias nao emitem alerta mesmo com valor critico`() {
        val e = engine.evaluate(ocupacao, series(14, 95.0, 0.0, endOffsetDays = 5), now)
        assertTrue(e is Evaluation.StaleData)
    }

    @Test fun `poucas leituras na janela retornam dados insuficientes`() {
        val e = engine.evaluate(ocupacao, series(5, 85.0, 2.0), now)
        assertEquals(Evaluation.InsufficientData(5, 7), e)
    }

    @Test fun `lacunas na serie reduzem a confianca`() {
        val full = engine.evaluate(ocupacao, series(14, 70.0, 1.5), now) as Evaluation.Raised
        val gaps = engine.evaluate(ocupacao, series(14, 70.0, 1.5, skip = setOf(1, 3, 5, 7, 9)), now) as Evaluation.Raised
        assertTrue(gaps.alert.confidence < full.alert.confidence)
    }

    @Test fun `regressao linear recupera inclinacao conhecida`() {
        val t = engine.linearTrend(series(10, 10.0, 2.0))
        assertEquals(2.0, t.slopePerDay, 1e-9)
        assertEquals(1.0, t.r2, 1e-9)
    }
}
