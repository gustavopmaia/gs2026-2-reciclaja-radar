package br.com.reciclaja.radar.data.prefs

import android.content.Context
import br.com.reciclaja.radar.domain.usecase.NotifiedAlertsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Preferências simples (SharedPreferences): território favorito, carga, permissões e alertas já notificados. */
class AppPreferences(context: Context) : NotifiedAlertsStore {
    private val sp = context.getSharedPreferences("radar_prefs", Context.MODE_PRIVATE)

    // Estado compartilhado entre telas (território e temas) com dono único: as preferências.
    private val _selectedTerritory = MutableStateFlow(sp.getString("territory", null))
    val selectedTerritory: StateFlow<String?> = _selectedTerritory
    fun selectTerritory(id: String) {
        sp.edit().putString("territory", id).apply()
        _selectedTerritory.value = id
    }

    private val _showClimate = MutableStateFlow(sp.getBoolean("show_climate", true))
    val showClimate: StateFlow<Boolean> = _showClimate
    fun setShowClimate(v: Boolean) {
        sp.edit().putBoolean("show_climate", v).apply()
        _showClimate.value = v
    }

    var lastLoadAt: Long?
        get() = sp.getLong("last_load", -1).takeIf { it > 0 }
        set(v) = sp.edit().putLong("last_load", v ?: -1).apply()

    var notificationPermissionAsked: Boolean
        get() = sp.getBoolean("notif_asked", false)
        set(v) = sp.edit().putBoolean("notif_asked", v).apply()

    override fun wasNotified(key: String) = sp.getStringSet("notified", emptySet())!!.contains(key)
    override fun markNotified(key: String) {
        val set = sp.getStringSet("notified", emptySet())!!.toMutableSet().apply { add(key) }
        sp.edit().putStringSet("notified", set).apply()
    }
}
