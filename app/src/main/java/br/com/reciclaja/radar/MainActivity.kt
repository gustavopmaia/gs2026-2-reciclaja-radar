package br.com.reciclaja.radar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.reciclaja.radar.ui.navigation.RadarNavHost
import br.com.reciclaja.radar.ui.theme.RadarTheme

class MainActivity : ComponentActivity() {
    // Destino vindo do toque na notificação (território, indicador).
    private var deepLink by mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) deepLink = intent.target()
        val container = (application as RadarApp).container
        setContent {
            RadarTheme {
                RadarNavHost(container, deepLink, onDeepLinkConsumed = { deepLink = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.target()?.let { deepLink = it }
    }

    private fun Intent.target(): Pair<String, String>? {
        val t = getStringExtra(EXTRA_TERRITORY) ?: return null
        val i = getStringExtra(EXTRA_INDICATOR) ?: return null
        return t to i
    }

    companion object {
        const val EXTRA_TERRITORY = "territoryId"
        const val EXTRA_INDICATOR = "indicatorId"
    }
}
