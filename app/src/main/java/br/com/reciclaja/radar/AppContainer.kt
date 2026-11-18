package br.com.reciclaja.radar

import android.app.Application
import android.content.Context
import androidx.room.Room
import br.com.reciclaja.radar.data.RoomRadarRepository
import br.com.reciclaja.radar.data.local.RadarDatabase
import br.com.reciclaja.radar.data.prefs.AppPreferences
import br.com.reciclaja.radar.domain.RadarRepository
import br.com.reciclaja.radar.domain.engine.AlertEngine
import br.com.reciclaja.radar.domain.usecase.BuildTerritoryOverviewUseCase
import br.com.reciclaja.radar.domain.usecase.NotifyCriticalAlertsUseCase
import br.com.reciclaja.radar.domain.usecase.RecordDecisionUseCase
import br.com.reciclaja.radar.platform.AndroidAlertNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Injeção manual de dependências: um lugar só monta o grafo (sem Hilt para manter o build enxuto). */
class AppContainer(context: Context) {
    val clock: () -> Long = { System.currentTimeMillis() }
    val prefs = AppPreferences(context)
    private val db = Room.databaseBuilder(context, RadarDatabase::class.java, "radar.db").build()
    val repository: RadarRepository = RoomRadarRepository(context, db.dao(), prefs, clock)
    val engine = AlertEngine()
    val buildOverview = BuildTerritoryOverviewUseCase(engine)
    val recordDecision = RecordDecisionUseCase(repository, clock)
    val notifier = AndroidAlertNotifier(context)
    val notifyCritical = NotifyCriticalAlertsUseCase(notifier, prefs)
}

class RadarApp : Application() {
    lateinit var container: AppContainer
        private set
    val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Carga inicial fora da thread principal; as telas mostram "Carregando" até o Room emitir.
        appScope.launch { container.repository.ensureSeeded() }
    }
}
