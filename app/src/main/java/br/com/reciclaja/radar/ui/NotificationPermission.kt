package br.com.reciclaja.radar.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import br.com.reciclaja.radar.AppContainer

/** Os três caminhos da permissão + "ainda não pedida". */
enum class NotifPermission(val label: String) {
    GRANTED("Concedida"),
    NOT_ASKED("Ainda não solicitada"),
    DENIED("Negada"),
    PERMANENTLY_DENIED("Negada permanentemente"),
}

class NotificationPermissionState(val status: NotifPermission, val request: () -> Unit)

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/**
 * Pede a permissão só quando o usuário toca no botão (com o motivo na tela).
 * Negada para sempre => o mesmo botão abre as configurações do sistema (atalho).
 */
@Composable
fun rememberNotificationPermission(container: AppContainer, onChanged: () -> Unit = {}): NotificationPermissionState {
    val context = LocalContext.current
    fun compute(): NotifPermission {
        if (container.notifier.canNotify()) return NotifPermission.GRANTED
        if (Build.VERSION.SDK_INT < 33) return NotifPermission.PERMANENTLY_DENIED // desligada nas configurações
        if (!container.prefs.notificationPermissionAsked) return NotifPermission.NOT_ASKED
        val rationale = context.activity()?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ?: false
        return if (rationale) NotifPermission.DENIED else NotifPermission.PERMANENTLY_DENIED
    }
    var status by remember { mutableStateOf(compute()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = compute(); onChanged() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.prefs.notificationPermissionAsked = true
        status = compute()
        onChanged()
    }
    return NotificationPermissionState(status) {
        if (status == NotifPermission.PERMANENTLY_DENIED || Build.VERSION.SDK_INT < 33) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
