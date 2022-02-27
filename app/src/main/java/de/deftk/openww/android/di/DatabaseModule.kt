package de.deftk.openww.android.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.deftk.openww.android.room.AppDatabase
import de.deftk.openww.android.room.IgnoredTaskDao
import de.deftk.openww.android.room.QuickMessageDao
import de.deftk.openww.android.room.migration.Migrate3To4
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(appContext, AppDatabase::class.java, "content-db")
            .addMigrations(Migrate3To4())
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideQuickMessageDao(appDatabase: AppDatabase): QuickMessageDao {
        return appDatabase.quickMessageDao()
    }

    @Provides
    @Singleton
    fun provideIgnoredTaskDao(appDatabase: AppDatabase): IgnoredTaskDao {
        return appDatabase.ignoredTaskDao()
    }

}