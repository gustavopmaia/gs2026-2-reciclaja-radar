package br.com.reciclaja.radar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import br.com.reciclaja.radar.domain.model.Reading
import br.com.reciclaja.radar.ui.asDayMonth
import br.com.reciclaja.radar.ui.fmt
import br.com.reciclaja.radar.ui.theme.LocalStatusColors
import br.com.reciclaja.radar.ui.theme.Spacing
import kotlin.math.max
import kotlin.math.min

/** Série temporal com previsão tracejada e linhas de limiar. Canvas próprio, sem biblioteca de gráfico. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrendChart(
    title: String,
    history: List<Reading>,
    forecast: List<Pair<Long, Double>>,
    attention: Double,
    critical: Double,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val status = LocalStatusColors.current
    val line = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = axis)

    val summary = if (history.isEmpty()) "$title: sem leituras." else {
        val last = history.last()
        "$title. ${history.size} leituras de ${history.first().timestamp.asDayMonth()} a ${last.timestamp.asDayMonth()}. " +
            "Último valor ${last.value.fmt()} $unit. " +
            (forecast.lastOrNull()?.let { "Previsão ${it.second.fmt()} $unit em ${it.first.asDayMonth()}. " } ?: "Sem previsão. ") +
            "Limiar de atenção ${attention.fmt()}, crítico ${critical.fmt()}."
    }

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Spacing.md)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.sm))
            Box(Modifier.fillMaxWidth().height(200.dp).semantics { contentDescription = summary }) {
                if (history.size < 2) {
                    Text("Leituras insuficientes para o gráfico.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.Center))
                } else Canvas(Modifier.fillMaxWidth().height(200.dp)) {
                    val allT = history.map { it.timestamp } + forecast.map { it.first }
                    val allV = history.map { it.value } + forecast.map { it.second } + listOf(attention, critical)
                    val tMin = allT.min().toFloat(); val tMax = allT.max().toFloat()
                    val vMin = max(0.0, allV.min() * 0.85).toFloat(); val vMax = (allV.max() * 1.05).toFloat()
                    val left = 44.dp.toPx(); val bottom = size.height - 20.dp.toPx(); val top = 6.dp.toPx(); val right = size.width - 4.dp.toPx()
                    fun x(t: Long) = left + (t - tMin) / max(1f, tMax - tMin) * (right - left)
                    fun y(v: Double) = bottom - (v.toFloat() - vMin) / max(0.0001f, vMax - vMin) * (bottom - top)

                    // eixos
                    drawLine(axis, Offset(left, top), Offset(left, bottom), 1.dp.toPx())
                    drawLine(axis, Offset(left, bottom), Offset(right, bottom), 1.dp.toPx())
                    drawText(measurer, vMax.toDouble().fmt(0), Offset(0f, top - 4.dp.toPx()), labelStyle)
                    drawText(measurer, vMin.toDouble().fmt(0), Offset(0f, bottom - 14.dp.toPx()), labelStyle)
                    drawText(measurer, history.first().timestamp.asDayMonth(), Offset(left, bottom + 2.dp.toPx()), labelStyle)
                    val endLabel = (forecast.lastOrNull()?.first ?: history.last().timestamp).asDayMonth()
                    drawText(measurer, endLabel, Offset(right - 36.dp.toPx(), bottom + 2.dp.toPx()), labelStyle)

                    // limiares
                    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    drawLine(status.attention, Offset(left, y(attention)), Offset(right, y(attention)), 2.dp.toPx(), pathEffect = dash)
                    drawLine(status.critical, Offset(left, y(critical)), Offset(right, y(critical)), 2.dp.toPx(), pathEffect = dash)

                    // histórico
                    val p = Path()
                    history.forEachIndexed { i, r -> if (i == 0) p.moveTo(x(r.timestamp), y(r.value)) else p.lineTo(x(r.timestamp), y(r.value)) }
                    drawPath(p, line, style = Stroke(3.dp.toPx()))
                    history.forEach { drawCircle(line, 3.dp.toPx(), Offset(x(it.timestamp), y(it.value))) }

                    // previsão
                    if (forecast.size >= 2) {
                        val f = Path()
                        forecast.forEachIndexed { i, (t, v) -> if (i == 0) f.moveTo(x(t), y(v)) else f.lineTo(x(t), y(v)) }
                        drawPath(f, axis, style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Legend(line, "Histórico ($unit)")
                Legend(axis, "Previsão", dashed = true)
                Legend(status.attention, "Atenção ${attention.fmt()}", dashed = true)
                Legend(status.critical, "Crítico ${critical.fmt()}", dashed = true)
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 18.dp, height = 10.dp)) {
            drawLine(
                color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 3.dp.toPx(),
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) else null,
            )
        }
        Spacer(Modifier.width(Spacing.xs))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
