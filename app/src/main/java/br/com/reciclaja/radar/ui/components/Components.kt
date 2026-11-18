package br.com.reciclaja.radar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.reciclaja.radar.domain.model.Alert
import br.com.reciclaja.radar.domain.model.AlertLevel
import br.com.reciclaja.radar.domain.model.Justification
import br.com.reciclaja.radar.domain.model.Territory
import br.com.reciclaja.radar.ui.asDateTime
import br.com.reciclaja.radar.ui.fmt
import br.com.reciclaja.radar.ui.theme.LocalStatusColors
import br.com.reciclaja.radar.ui.theme.Spacing

// Componentes só descrevem: recebem dados por parâmetro e devolvem eventos por callback. Nenhuma regra de negócio aqui.

fun levelIcon(level: AlertLevel?): ImageVector = when (level) {
    null -> Icons.Filled.CheckCircle
    AlertLevel.INFORMATIVO -> Icons.Filled.Info
    AlertLevel.ATENCAO, AlertLevel.CRITICO -> Icons.Filled.Warning
}

/** Ícone + rótulo: a cor nunca comunica o nível sozinha. */
@Composable
fun LevelBadge(level: AlertLevel?, modifier: Modifier = Modifier) {
    val color = LocalStatusColors.current.of(level)
    val label = level?.label ?: "Normal"
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "Nível $label" },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, color),
    ) {
        Row(Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Icon(levelIcon(level), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

/** Selo de origem do dado — dados sintéticos sempre identificados na tela. */
@Composable
fun OriginTag(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(top = Spacing.sm).semantics { heading() },
    )
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    unit: String,
    variationPct: Double?,
    level: AlertLevel?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val trend = variationPct?.let { (if (it >= 0) "▲ +" else "▼ ") + "${it.fmt()}% em 7 dias" } ?: "sem comparação semanal"
    val description = "$title: $value $unit, $trend, nível ${level?.label ?: "normal"}"
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(Spacing.md).fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(Spacing.xs))
                Text(unit, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(trend, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(top = Spacing.sm))
            LevelBadge(level)
        }
    }
    val m = modifier
        .heightIn(min = 48.dp)
        .semantics(mergeDescendants = true) { contentDescription = description }
    if (onClick != null) Card(onClick = onClick, modifier = m, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { content() }
    else Card(modifier = m, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { content() }
}

@Composable
fun AlertCard(alert: Alert, modifier: Modifier = Modifier, subtitle: String? = null, onClick: () -> Unit) {
    val color = LocalStatusColors.current.of(alert.level)
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Alerta ${alert.level.label}. ${alert.title}. Confiança ${alert.confidenceLabel}. Toque para ver a justificativa."
                role = Role.Button
            },
        border = BorderStroke(2.dp, color),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.Top) {
            Icon(levelIcon(alert.level), contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                LevelBadge(alert.level)
                Spacer(Modifier.padding(top = Spacing.xs))
                Text(alert.title, style = MaterialTheme.typography.titleSmall)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Confiança ${alert.confidenceLabel} (${(alert.confidence * 100).fmt(0)}%) · ver justificativa",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Mostra variáveis, regra, período e fonte que originaram o alerta. */
@Composable
fun JustificationPanel(justification: Justification, confidenceLabel: String, confidencePct: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Por que este alerta?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            justification.variables.forEach { (name, value) ->
                Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(Spacing.sm))
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f))
                }
            }
            HorizontalDivider()
            LabeledText("Regra", justification.rule)
            LabeledText("Período", "${justification.periodStart.asDateTime()} a ${justification.periodEnd.asDateTime()}")
            LabeledText("Fonte", "${justification.source} · atualizado em ${justification.updatedAt.asDateTime()}")
            LabeledText("Confiança", "$confidenceLabel (${(confidencePct * 100).fmt(0)}%) = ajuste da tendência × completude da janela")
        }
    }
}

@Composable
fun LabeledText(label: String, text: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerritorySelector(territories: List<Territory>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        territories.forEach { t ->
            val selected = t.id == selectedId
            FilterChip(
                selected = selected,
                onClick = { onSelect(t.id) },
                label = { Text("${t.name} · ${t.type.label}") },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { stateDescription = if (selected) "selecionado" else "não selecionado" },
            )
        }
    }
}

enum class StateKind { LOADING, EMPTY, ERROR, STALE }

/** Padroniza carregando, vazio, erro e aviso de dados desatualizados. */
@Composable
fun StateView(kind: StateKind, message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    val status = LocalStatusColors.current
    val (icon, color) = when (kind) {
        StateKind.LOADING -> null to MaterialTheme.colorScheme.primary
        StateKind.EMPTY -> Icons.Filled.Info to status.info
        StateKind.ERROR -> Icons.Filled.Warning to status.critical
        StateKind.STALE -> Icons.Filled.Warning to status.attention
    }
    Card(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        border = BorderStroke(1.5.dp, color),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            if (icon == null) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
            else Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    when (kind) {
                        StateKind.LOADING -> "Carregando"
                        StateKind.EMPTY -> "Sem dados"
                        StateKind.ERROR -> "Erro"
                        StateKind.STALE -> "Dados desatualizados"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                )
                Text(message, style = MaterialTheme.typography.bodyMedium)
                if (onRetry != null) {
                    OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = Spacing.sm).heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Tentar novamente")
                    }
                }
            }
        }
    }
}
