package com.poke86.game.di

import com.poke86.game.data.repository.TDSaveRepositoryImpl
import com.poke86.game.domain.repository.TDSaveRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TDSaveModule {

    @Binds
    @Singleton
    abstract fun bindTDSaveRepository(impl: TDSaveRepositoryImpl): TDSaveRepository
}
