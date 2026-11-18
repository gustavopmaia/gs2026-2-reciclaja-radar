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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.reciclaja.radar.domain.model.Indicator
import br.com.reciclaja.radar.ui.NotifPermission
import br.com.reciclaja.radar.ui.NotificationPermissionState
import br.com.reciclaja.radar.ui.UiState
import br.com.reciclaja.radar.ui.asDateTime
import br.com.reciclaja.radar.ui.components.LabeledText
import br.com.reciclaja.radar.ui.components.OriginTag
import br.com.reciclaja.radar.ui.components.SectionTitle
import br.com.reciclaja.radar.ui.components.StateKind
import br.com.reciclaja.radar.ui.components.StateView
import br.com.reciclaja.radar.ui.components.TerritorySelector
import br.com.reciclaja.radar.ui.fmt
import br.com.reciclaja.radar.ui.theme.Spacing

private fun pad(p: PaddingValues) = PaddingValues(start = Spacing.md, end = Spacing.md, top = p.calculateTopPadding() + Spacing.sm, bottom = p.calculateBottomPadding() + Spacing.lg)

@Composable
fun HistoryScreen(vm: HistoryViewModel, padding: PaddingValues) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        item { SectionTitle("Decisões registradas") }
        when (val s = state) {
            UiState.Loading -> item { StateView(StateKind.LOADING, "Lendo histórico…") }
            is UiState.Empty -> item { StateView(StateKind.EMPTY, s.message) }
            is UiState.Error -> item { StateView(StateKind.ERROR, s.message) }
            is UiState.Stale -> Unit
            is UiState.Success -> items(s.data, key = { it.decision.id }) { row ->
                DecisionRow(row.decision, "${row.territoryName} · ${row.indicatorName}")
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel, permission: NotificationPermissionState, padding: PaddingValues, snackbar: SnackbarHostState) {
    val territories by vm.territories.collectAsStateWithLifecycle()
    val indicators by vm.indicators.collectAsStateWithLifecycle()
    val selected by vm.selectedId.collectAsStateWithLifecycle()
    val climate by vm.showClimate.collectAsStateWithLifecycle()
    val info by vm.info.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        item { SectionTitle("Território favorito") }
        item { TerritorySelector(territories, selected ?: territories.firstOrNull()?.id, onSelect = vm::select) }

        item { SectionTitle("Temas monitorados") }
        item {
            SettingCard {
                LabeledText("Resíduos", "Ocupação dos contêineres, descarte irregular e coleta (sempre ativo).")
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Clima (chuva) como variável auxiliar", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = climate, onCheckedChange = vm::setShowClimate)
                }
            }
        }

        item { SectionTitle("Notificações de alerta crítico") }
        item {
            SettingCard {
                LabeledText("Permissão", permission.status.label)
                Text(
                    when (permission.status) {
                        NotifPermission.GRANTED -> "Você recebe um aviso local quando surgir um alerta crítico (uma vez por alerta)."
                        NotifPermission.NOT_ASKED -> "Usamos a notificação só para alertas críticos. Sem ela, os alertas continuam visíveis no painel."
                        NotifPermission.DENIED -> "Negada. Os alertas críticos aparecem apenas no painel (plano B)."
                        NotifPermission.PERMANENTLY_DENIED -> "Negada permanentemente. Ative pelas configurações do sistema; até lá, o painel mostra os alertas."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (permission.status != NotifPermission.GRANTED) {
                    Button(onClick = permission.request, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (permission.status == NotifPermission.PERMANENTLY_DENIED) "Abrir configurações do sistema" else "Permitir notificações")
                    }
                }
            }
        }

        item { SectionTitle("Limiares (administrador)") }
        items(indicators, key = { it.id }) { ThresholdEditor(it, vm::saveThresholds) }

        item { SectionTitle("Fonte dos dados") }
        item {
            SettingCard {
                OriginTag("Dados sintéticos (fictícios)")
                val (src, last) = info
                LabeledText("Fonte", src?.source ?: "—")
                LabeledText("Licença", src?.license ?: "—")
                LabeledText("Versão da base", src?.version ?: "—")
                LabeledText("Última carga", last?.asDateTime() ?: "—")
                LabeledText("Observação", src?.note ?: "—")
                LabeledText("Política de atualização", "Offline-first: a base local é a fonte. Recarga a cada versão do app ou manualmente. Leituras com mais de 2 dias são marcadas como desatualizadas e não geram alerta.")
                OutlinedButton(onClick = vm::reload, modifier = Modifier.heightIn(min = 48.dp)) { Text("Recarregar base sintética") }
            }
        }

        item { SectionTitle("Privacidade (LGPD)") }
        item {
            SettingCard {
                Text(
                    "O app não coleta dados pessoais. Só o nome do responsável digitado nas decisões fica salvo, apenas neste aparelho, até você apagar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Apagar histórico de decisões") }
            }
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Apagar histórico?") },
        text = { Text("Todas as decisões registradas neste aparelho serão removidas. Não é possível desfazer.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearHistory() }) { Text("Apagar") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
    )
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) { content() }
    }
}

@Composable
private fun ThresholdEditor(ind: Indicator, onSave: (String, String, String) -> Unit) {
    var att by rememberSaveable(ind.id, ind.attentionThreshold) { mutableStateOf(ind.attentionThreshold.fmt()) }
    var crit by rememberSaveable(ind.id, ind.criticalThreshold) { mutableStateOf(ind.criticalThreshold.fmt()) }
    SettingCard {
        Text("${ind.name} (${ind.unit})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { })
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(att, { att = it }, label = { Text("Atenção") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Spacing.sm))
            OutlinedTextField(crit, { crit = it }, label = { Text("Crítico") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
        }
        OutlinedButton(onClick = { onSave(ind.id, att, crit) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Salvar limiares") }
    }
}
