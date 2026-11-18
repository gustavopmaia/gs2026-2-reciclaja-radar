package br.com.reciclaja.radar.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.reciclaja.radar.MainActivity
import br.com.reciclaja.radar.R
import br.com.reciclaja.radar.domain.model.Alert
import br.com.reciclaja.radar.domain.usecase.AlertNotifier

/** Adaptador do recurso nativo. A UI e o domínio nunca chamam a API de notificação diretamente. */
class AndroidAlertNotifier(private val context: Context) : AlertNotifier {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL, "Alertas críticos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alertas preditivos de nível crítico do ReciclaJá Radar"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun canNotify(): Boolean = hasPermission() && NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun notifyCritical(alert: Alert, territoryName: String) {
        if (!canNotify()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TERRITORY, alert.territoryId)
            putExtra(MainActivity.EXTRA_INDICATOR, alert.indicatorId)
        }
        val pi = PendingIntent.getActivity(context, alert.key.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle("Alerta crítico · $territoryName")
            .setContentText(alert.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${alert.title}\nConfiança ${alert.confidenceLabel}. Toque para ver a justificativa e registrar a decisão."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(alert.key.hashCode(), n)
        } catch (_: SecurityException) {
            // Permissão revogada entre a checagem e o envio: o banner na tela continua sendo o plano B.
        }
    }

    companion object { const val CHANNEL = "critical_alerts" }
}
