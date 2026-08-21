package com.secureguard.enterprise.di

import android.content.Context
import androidx.room.Room
import com.secureguard.enterprise.data.database.AgentConfigDao
import com.secureguard.enterprise.data.database.AlertDao
import com.secureguard.enterprise.data.database.AssetDao
import com.secureguard.enterprise.data.database.DetectionDao
import com.secureguard.enterprise.data.database.PendingCommandDao
import com.secureguard.enterprise.data.database.SecureGuardDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): SecureGuardDatabase =
        Room.databaseBuilder(
            ctx,
            SecureGuardDatabase::class.java,
            SecureGuardDatabase.DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAssetDao(db: SecureGuardDatabase): AssetDao = db.assetDao()

    @Provides
    fun provideDetectionDao(db: SecureGuardDatabase): DetectionDao = db.detectionDao()

    @Provides
    fun provideAlertDao(db: SecureGuardDatabase): AlertDao = db.alertDao()

    @Provides
    fun provideAgentConfigDao(db: SecureGuardDatabase): AgentConfigDao = db.agentConfigDao()

    @Provides
    fun providePendingCommandDao(db: SecureGuardDatabase): PendingCommandDao = db.pendingCommandDao()
}
