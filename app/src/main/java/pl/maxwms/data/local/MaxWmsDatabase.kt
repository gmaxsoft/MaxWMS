package pl.maxwms.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.maxwms.data.local.dao.PartDao
import pl.maxwms.data.local.dao.StockMovementDao
import pl.maxwms.data.local.entity.PartEntity
import pl.maxwms.data.local.entity.StockMovementEntity

@Database(
    entities = [PartEntity::class, StockMovementEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class MaxWmsDatabase : RoomDatabase() {
    abstract fun partDao(): PartDao

    abstract fun stockMovementDao(): StockMovementDao
}
