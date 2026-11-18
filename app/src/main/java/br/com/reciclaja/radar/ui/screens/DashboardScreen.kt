package br.com.reciclaja.radar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.reciclaja.radar.domain.model.AlertLevel
import br.com.reciclaja.radar.domain.model.Evaluation
import br.com.reciclaja.radar.domain.usecase.TerritoryOverview
import br.com.reciclaja.radar.ui.NotifPermission
import br.com.reciclaja.radar.ui.NotificationPermissionState
import br.com.reciclaja.radar.ui.UiState
import br.com.reciclaja.radar.ui.asDateTime
import br.com.reciclaja.radar.ui.components.AlertCard
import br.com.reciclaja.radar.ui.components.KpiCard
import br.com.reciclaja.radar.ui.components.OriginTag
import br.com.reciclaja.radar.ui.components.SectionTitle
import br.com.reciclaja.radar.ui.components.StateKind
import br.com.reciclaja.radar.ui.components.StateView
import br.com.reciclaja.radar.ui.components.TerritorySelector
import br.com.reciclaja.radar.ui.components.TrendChart
import br.com.reciclaja.radar.ui.fmt
import br.com.reciclaja.radar.ui.unavailableLabel
import br.com.reciclaja.radar.ui.relativeAge
import br.com.reciclaja.radar.ui.theme.LocalStatusColors
import br.com.reciclaja.radar.ui.theme.Spacing

@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    permission: NotificationPermissionState,
    padding: PaddingValues,
    onIndicator: (String, String) -> Unit,
    onAlert: (String, String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val territories by vm.territories.collectAsStateWithLifecycle()
    val selected by vm.selectedId.collectAsStateWithLifecycle()
    val lastLoad by vm.lastLoad.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp), // compacto: 2 colunas; expandido: 4+
        contentPadding = PaddingValues(
            start = Spacing.md, end = Spacing.md,
            top = padding.calculateTopPadding() + Spacing.sm, bottom = padding.calculateBottomPadding() + Spacing.lg,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        full { SectionTitle("Território") }
        full {
            TerritorySelector(territories, selected ?: territories.firstOrNull()?.id, onSelect = vm::select)
        }
        full {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OriginTag("Dados sintéticos (fictícios)")
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    lastLoad?.let { "Base carregada ${it.relativeAge(vm.now())}" } ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (val s = state) {
            UiState.Loading -> full { StateView(StateKind.LOADING, "Lendo o banco local…") }
            is UiState.Empty -> full { StateView(StateKind.EMPTY, s.message) }
            is UiState.Error -> full { StateView(StateKind.ERROR, s.message, onRetry = vm::retry) }
            is UiState.Stale -> {
                full {
                    StateView(
                        StateKind.STALE,
                        "Última leitura em ${s.lastUpdate.asDateTime()} (${s.lastUpdate.relativeAge(vm.now())}). " +
                            "Com dados velhos o app não emite alertas novos — confirme em campo antes de decidir.",
                    )
                }
                overviewContent(s.data, permission, onIndicator, onAlert)
            }
            is UiState.Success -> overviewContent(s.data, permission, onIndicator, onAlert)
        }
    }
}

private fun LazyGridScope.full(content: @Composable () -> Unit) = item(span = { GridItemSpan(maxLineSpan) }) { content() }

private fun LazyGridScope.overviewContent(
    ov: TerritoryOverview,
    permission: NotificationPermissionState,
    onIndicator: (String, String) -> Unit,
    onAlert: (String, String) -> Unit,
) {
    val hasCritical = ov.alerts.any { it.level == AlertLevel.CRITICO }
    if (hasCritical && permission.status != NotifPermission.GRANTED) full { NotificationFallbackBanner(permission) }

    full { SectionTitle("Alertas ativos (${ov.alerts.size})") }
    if (ov.alerts.isEmpty()) full {
        Text(
            "Nenhum alerta para ${ov.territory.name}. Indicadores dentro dos limiares ou sem dados suficientes.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    ov.alerts.forEach { a ->
        val name = ov.items.first { it.indicator.id == a.indicatorId }.indicator.name
        full { AlertCard(a, subtitle = "$name · ${ov.territory.name}", onClick = { onAlert(a.territoryId, a.indicatorId) }) }
    }

    full { SectionTitle("Indicadores") }
    items(ov.items, key = { it.indicator.id }) { st ->
        val level = (st.evaluation as? Evaluation.Raised)?.alert?.level
        KpiCard(
            title = st.indicator.name,
            value = st.latest?.value?.fmt() ?: "—",
            unit = st.indicator.unit,
            variationPct = st.weeklyVariationPct,
            level = level,
            unavailable = st.evaluation.unavailableLabel(),
            onClick = { onIndicator(ov.territory.id, st.indicator.id) },
        )
    }
    ov.items.firstOrNull { it.indicator.id == "ocupacao" }?.let { st ->
        full {
            TrendChart(
                title = "Tendência: ${st.indicator.name}",
                history = st.history.takeLast(14),
                forecast = st.forecast,
                attention = st.indicator.attentionThreshold,
                critical = st.indicator.criticalThreshold,
                unit = st.indicator.unit,
            )
        }
    }
}

/** Plano B do recurso nativo: sem permissão, o alerta crítico continua visível no painel. */
@Composable
private fun NotificationFallbackBanner(permission: NotificationPermissionState) {
    val color = LocalStatusColors.current.critical
    Card(
        border = BorderStroke(1.5.dp, color),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = color)
                Spacer(Modifier.width(Spacing.sm))
                Text("Notificações desativadas (${permission.status.label})", style = MaterialTheme.typography.titleSmall)
            }
            Text(
                "Há alerta crítico neste território. Sem permissão, você só será avisado aqui no painel. " +
                    "Ative para receber o aviso mesmo com o app fechado.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = permission.request, modifier = Modifier.padding(top = Spacing.sm).heightIn(min = 48.dp)) {
                Text(if (permission.status == NotifPermission.PERMANENTLY_DENIED) "Abrir configurações do sistema" else "Ativar notificações")
            }
        }
    }
}
