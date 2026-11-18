package br.com.reciclaja.radar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.reciclaja.radar.domain.model.AlertLevel
import br.com.reciclaja.radar.domain.model.Decision
import br.com.reciclaja.radar.domain.model.Evaluation
import br.com.reciclaja.radar.domain.usecase.IndicatorStatus
import br.com.reciclaja.radar.domain.usecase.TerritoryOverview
import br.com.reciclaja.radar.ui.UiState
import br.com.reciclaja.radar.ui.asDateTime
import br.com.reciclaja.radar.ui.components.AlertCard
import br.com.reciclaja.radar.ui.components.DecisionSheet
import br.com.reciclaja.radar.ui.components.JustificationPanel
import br.com.reciclaja.radar.ui.components.KpiCard
import br.com.reciclaja.radar.ui.components.LabeledText
import br.com.reciclaja.radar.ui.components.LevelBadge
import br.com.reciclaja.radar.ui.components.OriginTag
import br.com.reciclaja.radar.ui.components.SectionTitle
import br.com.reciclaja.radar.ui.components.StateKind
import br.com.reciclaja.radar.ui.components.StateView
import br.com.reciclaja.radar.ui.components.TrendChart
import br.com.reciclaja.radar.ui.fmt
import br.com.reciclaja.radar.ui.unavailableLabel
import br.com.reciclaja.radar.ui.relativeAge
import br.com.reciclaja.radar.ui.theme.Spacing

/** Renderiza os estados comuns e entrega o overview pronto ao conteúdo. */
private fun LazyListScope.withOverview(
    state: UiState<TerritoryOverview>,
    now: Long,
    onRetry: () -> Unit,
    content: LazyListScope.(TerritoryOverview) -> Unit,
) {
    when (state) {
        UiState.Loading -> item { StateView(StateKind.LOADING, "Lendo o banco local…") }
        is UiState.Empty -> item { StateView(StateKind.EMPTY, state.message) }
        is UiState.Error -> item { StateView(StateKind.ERROR, state.message, onRetry = onRetry) }
        is UiState.Stale -> {
            item { StateView(StateKind.STALE, "Última leitura em ${state.lastUpdate.asDateTime()} (${state.lastUpdate.relativeAge(now)}).") }
            content(state.data)
        }
        is UiState.Success -> content(state.data)
    }
}

private fun pad(p: PaddingValues) = PaddingValues(start = Spacing.md, end = Spacing.md, top = p.calculateTopPadding() + Spacing.sm, bottom = p.calculateBottomPadding() + Spacing.lg)

@Composable
fun IndicatorDetailScreen(vm: DetailViewModel, padding: PaddingValues, onAlert: (String, String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        withOverview(state, vm.now(), vm::retry) { ov ->
            val st = vm.status(ov) ?: run { item { StateView(StateKind.EMPTY, "Indicador não encontrado.") }; return@withOverview }
            val alert = vm.alert(ov)
            item {
                KpiCard(
                    title = "${st.indicator.name} · ${ov.territory.name}",
                    value = st.latest?.value?.fmt() ?: "—",
                    unit = st.indicator.unit,
                    variationPct = st.weeklyVariationPct,
                    level = alert?.level,
                    unavailable = st.evaluation.unavailableLabel(),
                )
            }
            item { Text(st.indicator.description, style = MaterialTheme.typography.bodyMedium) }
            item {
                TrendChart(
                    title = "Histórico (30 dias) e previsão (3 dias)",
                    history = st.history,
                    forecast = st.forecast,
                    attention = st.indicator.attentionThreshold,
                    critical = st.indicator.criticalThreshold,
                    unit = st.indicator.unit,
                )
            }
            item { SectionTitle("Situação da previsão") }
            when (val e = st.evaluation) {
                is Evaluation.Raised -> {
                    item { AlertCard(e.alert, onClick = { onAlert(ov.territory.id, st.indicator.id) }) }
                    item { JustificationPanel(e.alert.justification, e.alert.confidenceLabel, e.alert.confidence) }
                }
                else -> item {
                    StateView(
                        if (e is Evaluation.StaleData) StateKind.STALE else StateKind.EMPTY,
                        noAlertReason(e, st),
                        title = when (e) { Evaluation.Normal -> "Sem alerta"; is Evaluation.InsufficientData -> "Dados insuficientes"; else -> null },
                    )
                }
            }
            item { SectionTitle("Últimas leituras") }
            item { ReadingsTable(st) }
        }
    }
}

fun noAlertReason(e: Evaluation, st: IndicatorStatus): String = when (e) {
    Evaluation.NoData -> "Sem leituras: nenhum alerta é emitido sem dados."
    Evaluation.Normal -> "Dentro dos limiares (atenção ${st.indicator.attentionThreshold.fmt()} ${st.indicator.unit}). Nenhum alerta."
    is Evaluation.InsufficientData -> "Dados insuficientes: ${e.available} leituras nos últimos 14 dias (mínimo ${e.required}). O app não emite alerta com base fraca."
    is Evaluation.StaleData -> "Última leitura em ${e.lastReadingAt.asDateTime()}. Com dados com mais de 2 dias o app não emite alerta."
    is Evaluation.Raised -> ""
}

@Composable
private fun ReadingsTable(st: IndicatorStatus) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Spacing.md)) {
            if (st.history.isEmpty()) Text("Sem leituras.")
            st.history.takeLast(7).reversed().forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs).semantics(mergeDescendants = true) {}) {
                    Text(r.timestamp.asDateTime(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("${r.value.fmt()} ${st.indicator.unit}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    OriginTag(r.origin.label)
                }
            }
        }
    }
}

private fun recommendation(level: AlertLevel) = when (level) {
    AlertLevel.CRITICO -> "Sugestão: antecipar a coleta no ponto crítico e avisar os moradores. Mudança de rota ou equipe extra exige sua confirmação."
    AlertLevel.ATENCAO -> "Sugestão: reforçar o monitoramento e verificar o ponto na próxima ronda."
    AlertLevel.INFORMATIVO -> "Sugestão: acompanhar a tendência; nenhuma ação imediata."
}

@Composable
fun AlertDetailScreen(vm: DetailViewModel, padding: PaddingValues, snackbar: SnackbarHostState) {
    val state by vm.state.collectAsStateWithLifecycle()
    val decisions by vm.decisions.collectAsStateWithLifecycle()
    val error by vm.decisionError.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(saved) {
        if (saved) { sheetOpen = false; vm.consumeSaved(); snackbar.showSnackbar("Decisão registrada no histórico.") }
    }

    val ov = when (val s = state) { is UiState.Success -> s.data; is UiState.Stale -> s.data; else -> null }
    val alert = ov?.let { vm.alert(it) }

    LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        withOverview(state, vm.now(), vm::retry) { o ->
            val st = vm.status(o)
            if (alert == null || st == null) {
                item {
                    StateView(StateKind.EMPTY, "Este alerta não está mais ativo: " + (st?.let { noAlertReason(it.evaluation, it) } ?: "indicador não encontrado."))
                }
                return@withOverview
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row { LevelBadge(alert.level); Spacer(Modifier.width(Spacing.sm)); OriginTag("Dados sintéticos") }
                        Text(alert.title, style = MaterialTheme.typography.titleMedium)
                        Text("${o.territory.name} · ${alert.summary}", style = MaterialTheme.typography.bodyMedium)
                        LabeledText("Confiança", "${alert.confidenceLabel} (${(alert.confidence * 100).fmt(0)}%)")
                        Text(recommendation(alert.level), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { JustificationPanel(alert.justification, alert.confidenceLabel, alert.confidence) }
            item {
                TrendChart(
                    title = "Dados que originaram o alerta",
                    history = st.history.takeLast(14),
                    forecast = st.forecast,
                    attention = st.indicator.attentionThreshold,
                    critical = st.indicator.criticalThreshold,
                    unit = st.indicator.unit,
                )
            }
            item {
                Button(onClick = { vm.clearError(); sheetOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Registrar decisão (acatar, adiar ou descartar)")
                }
            }
            item { SectionTitle("Decisões sobre este indicador (${decisions.size})") }
            if (decisions.isEmpty()) item { Text("Nenhuma decisão registrada ainda.", style = MaterialTheme.typography.bodyMedium) }
            decisions.forEach { d -> item { DecisionRow(d, null) } }
        }
    }

    if (sheetOpen && alert != null) {
        DecisionSheet(
            alert = alert,
            error = error,
            onDismiss = { sheetOpen = false },
            onEdited = vm::clearError,
            onConfirm = { action, who, note -> vm.decide(alert, action, who, note) },
        )
    }
}

@Composable
fun DecisionRow(d: Decision, context: String?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md).semantics(mergeDescendants = true) {}) {
            Row {
                Text(d.action.label, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(Spacing.sm))
                LevelBadge(d.alertLevel)
            }
            if (context != null) Text(context, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(d.alertTitle, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(Modifier.padding(vertical = Spacing.xs))
            Text("${d.createdAt.asDateTime()} · Responsável: ${d.responsible}", style = MaterialTheme.typography.labelMedium)
            if (d.note.isNotBlank()) Text("Observação: ${d.note}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
