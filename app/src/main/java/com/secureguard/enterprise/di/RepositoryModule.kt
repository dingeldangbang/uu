package com.secureguard.enterprise.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * DI-Modul für das Daten-Layer.
 *
 * Die Repositories (`SecureGuardRepository`, `SettingsRepository`,
 * `SeedDataInitializer`) sind alle `@Singleton` mit `@Inject constructor`,
 * sodass Hilt sie automatisch bereitstellt.
 *
 * Dieses Modul dokumentiert die aktiven Repository-Bindungen an einer Stelle.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Aktuell aktiv gebundene Repositories (alle Singleton-konstruiert):
     *  - com.secureguard.enterprise.data.repository.SecureGuardRepository
     *  - com.secureguard.enterprise.data.repository.SettingsRepository
     *  - com.secureguard.enterprise.data.repository.SeedDataInitializer
     *
     * Falls ein Repository gegen ein Interface getauscht werden soll,
     * hier @Binds-Deklarationen ergänzen — Consumer bleiben unverändert.
     */
    const val ACTIVE_REPOSITORIES: String =
        "SecureGuardRepository, SettingsRepository, SeedDataInitializer"
}
