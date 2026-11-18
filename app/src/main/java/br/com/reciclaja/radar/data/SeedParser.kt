package br.com.reciclaja.radar.data

import br.com.reciclaja.radar.data.local.IndicatorEntity
import br.com.reciclaja.radar.data.local.ReadingEntity
import br.com.reciclaja.radar.data.local.TerritoryEntity
import br.com.reciclaja.radar.domain.SourceInfo
import br.com.reciclaja.radar.domain.model.DAY_MS
import org.json.JSONObject

/** Converte o seed.json (assets) em entidades, reancorando dayOffset na data da carga. */
object SeedParser {
    data class Seed(
        val info: SourceInfo,
        val territories: List<TerritoryEntity>,
        val indicators: List<IndicatorEntity>,
        val readings: List<ReadingEntity>,
    )

    fun parse(json: String, anchor: Long): Seed {
        val root = JSONObject(json)
        val meta = root.getJSONObject("meta")
        val t = root.getJSONArray("territories")
        val i = root.getJSONArray("indicators")
        val r = root.getJSONArray("readings")
        return Seed(
            info = SourceInfo(
                source = meta.getString("fonte"),
                license = meta.getString("licenca"),
                origin = meta.getString("origem"),
                version = meta.getString("versao"),
                note = meta.getString("inspiracao"),
            ),
            territories = (0 until t.length()).map { t.getJSONObject(it) }.map {
                TerritoryEntity(it.getString("id"), it.getString("name"), it.getString("type"), it.getDouble("lat"), it.getDouble("lon"))
            },
            indicators = (0 until i.length()).map { i.getJSONObject(it) }.map {
                IndicatorEntity(
                    it.getString("id"), it.getString("theme"), it.getString("name"), it.getString("unit"),
                    it.getDouble("attention"), it.getDouble("critical"), it.getString("description"),
                )
            },
            readings = (0 until r.length()).map { r.getJSONObject(it) }.map {
                ReadingEntity(
                    territoryId = it.getString("territoryId"),
                    indicatorId = it.getString("indicatorId"),
                    timestamp = anchor + it.getInt("dayOffset") * DAY_MS,
                    value = it.getDouble("value"),
                    origin = it.getString("origin"),
                )
            },
        )
    }
}
