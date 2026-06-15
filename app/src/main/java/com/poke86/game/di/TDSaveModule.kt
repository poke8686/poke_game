package com.poke86.game.di

import com.poke86.game.data.datasource.remote.TDRemoteDataSource
import com.poke86.game.data.datasource.remote.TDRemoteDataSourceImpl
import com.poke86.game.data.repository.TDSaveRepositoryImpl
import com.poke86.game.domain.repository.TDSaveRepository
import com.poke86.game.network.TDApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TDSaveModule {

    @Binds
    @Singleton
    abstract fun bindTDSaveRepository(impl: TDSaveRepositoryImpl): TDSaveRepository

    @Binds
    @Singleton
    abstract fun bindTDRemoteDataSource(impl: TDRemoteDataSourceImpl): TDRemoteDataSource

    companion object {
        @Provides
        @Singleton
        fun provideTDApi(): TDApi = TDApi()
    }
}
