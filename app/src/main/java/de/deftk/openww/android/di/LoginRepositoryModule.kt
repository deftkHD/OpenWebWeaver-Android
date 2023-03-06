package de.deftk.openww.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.deftk.openww.android.repository.login.LoginRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class LoginRepositoryModule {

    @Provides
    @Singleton
    fun provideLoginRepository(): LoginRepository {
        return LoginRepository()
    }

}