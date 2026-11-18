package br.com.reciclaja.radar.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "territory")
data class TerritoryEntity(@PrimaryKey val id: String, val name: String, val type: String, val lat: Double, val lon: Double)

@Entity(tableName = "indicator")
data class IndicatorEntity(
    @PrimaryKey val id: String,
    val theme: String,
    val name: String,
    val unit: String,
    val attention: Double,
    val critical: Double,
    val description: String,
)

@Entity(tableName = "reading", indices = [Index("territoryId", "indicatorId", "timestamp")])
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val territoryId: String,
    val indicatorId: String,
    val timestamp: Long,
    val value: Double,
    val origin: String,
)

@Entity(tableName = "decision")
data class DecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alertKey: String,
    val territoryId: String,
    val indicatorId: String,
    val alertLevel: String,
    val alertTitle: String,
    val action: String,
    val responsible: String,
    val note: String,
    val createdAt: Long,
)

@Dao
interface RadarDao {
    @Query("SELECT * FROM territory ORDER BY name") fun territories(): Flow<List<TerritoryEntity>>
    @Query("SELECT * FROM indicator ORDER BY rowid") fun indicators(): Flow<List<IndicatorEntity>>
    @Query("SELECT * FROM reading WHERE territoryId = :territoryId ORDER BY timestamp") fun readings(territoryId: String): Flow<List<ReadingEntity>>
    @Query("SELECT * FROM decision ORDER BY createdAt DESC") fun decisions(): Flow<List<DecisionEntity>>
    @Query("SELECT COUNT(*) FROM territory") suspend fun territoryCount(): Int
    @Insert suspend fun insertDecision(d: DecisionEntity): Long
    @Query("DELETE FROM decision") suspend fun clearDecisions()
    @Query("UPDATE indicator SET attention = :attention, critical = :critical WHERE id = :id")
    suspend fun updateThresholds(id: String, attention: Double, critical: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTerritories(t: List<TerritoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertIndicators(i: List<IndicatorEntity>)
    @Insert suspend fun insertReadings(r: List<ReadingEntity>)
    @Query("DELETE FROM reading") suspend fun clearReadings()

    /** Carga atômica: ou entra a base inteira, ou nada (evita gravação parcial). */
    @Transaction
    suspend fun replaceSeed(t: List<TerritoryEntity>, i: List<IndicatorEntity>, r: List<ReadingEntity>) {
        clearReadings()
        insertTerritories(t)
        insertIndicators(i)
        insertReadings(r)
    }
}

@Database(
    entities = [TerritoryEntity::class, IndicatorEntity::class, ReadingEntity::class, DecisionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RadarDatabase : RoomDatabase() {
    abstract fun dao(): RadarDao
}
