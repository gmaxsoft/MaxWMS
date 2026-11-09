package pl.maxwms.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.maxwms.data.local.MIGRATION_1_2
import pl.maxwms.data.local.MaxWmsDatabase
import pl.maxwms.data.local.dao.PartDao
import pl.maxwms.data.local.dao.StockMovementDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MaxWmsDatabase =
        Room.databaseBuilder(context, MaxWmsDatabase::class.java, "maxwms.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun providePartDao(database: MaxWmsDatabase): PartDao = database.partDao()

    @Provides
    fun provideMovementDao(database: MaxWmsDatabase): StockMovementDao = database.stockMovementDao()
}
