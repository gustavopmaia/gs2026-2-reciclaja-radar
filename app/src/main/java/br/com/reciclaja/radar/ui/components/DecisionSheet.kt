package br.com.reciclaja.radar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.reciclaja.radar.domain.model.Alert
import br.com.reciclaja.radar.domain.model.DecisionAction
import br.com.reciclaja.radar.ui.theme.Spacing

/** Registro da decisão humana. A validação fica no caso de uso; aqui só coleta e repassa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionSheet(
    alert: Alert,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (DecisionAction, String, String) -> Unit,
) {
    var action by rememberSaveable { mutableStateOf(DecisionAction.ACATAR) }
    var responsible by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .padding(horizontal = Spacing.md)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Registrar decisão", style = MaterialTheme.typography.titleMedium)
            Text(alert.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                "O app apenas recomenda. Ações de alto impacto (mudar rota, acionar equipe extra) são decididas por você.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                DecisionAction.entries.forEachIndexed { i, a ->
                    SegmentedButton(
                        selected = action == a,
                        onClick = { action = a },
                        shape = SegmentedButtonDefaults.itemShape(i, DecisionAction.entries.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(a.label) }
                }
            }
            OutlinedTextField(
                value = responsible, onValueChange = { responsible = it.take(80) },
                label = { Text("Responsável") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note, onValueChange = { note = it.take(500) },
                label = { Text(if (action == DecisionAction.ACATAR) "Observação (opcional)" else "Motivo (obrigatório)") },
                minLines = 2, modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { onConfirm(action, responsible, note) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(bottom = Spacing.md),
            ) { Text("Confirmar: ${action.label}") }
        }
    }
}
