package pl.maxwms.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.maxwms.data.repository.LicenseRepositoryImpl
import pl.maxwms.data.repository.PartRepositoryImpl
import pl.maxwms.data.repository.SettingsRepositoryImpl
import pl.maxwms.data.sync.CatalogSynchronizer
import pl.maxwms.domain.repository.CatalogSync
import pl.maxwms.domain.repository.LicenseRepository
import pl.maxwms.domain.repository.PartRepository
import pl.maxwms.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindParts(impl: PartRepositoryImpl): PartRepository

    @Binds
    @Singleton
    abstract fun bindLicense(impl: LicenseRepositoryImpl): LicenseRepository

    @Binds
    @Singleton
    abstract fun bindSettings(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSync(impl: CatalogSynchronizer): CatalogSync
}
