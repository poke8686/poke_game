package com.poke86.game.di

import com.poke86.game.network.SpotDiffApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpotDiffModule {

    @Provides
    @Singleton
    fun provideSpotDiffApi(): SpotDiffApi = SpotDiffApi()
}
