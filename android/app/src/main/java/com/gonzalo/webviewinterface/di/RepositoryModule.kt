package com.gonzalo.webviewinterface.di

import com.gonzalo.webviewinterface.data.location.LocationRepositoryImpl
import com.gonzalo.webviewinterface.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * domain 레이어의 Repository 인터페이스를 data 레이어 구현체와 바인딩한다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
}
