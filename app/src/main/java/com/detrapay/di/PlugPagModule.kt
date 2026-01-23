package com.detrapay.di

import android.content.Context
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlugPagModule {

    @Provides
    @Singleton
    fun providePlugPagModule(context: Context): IPlugPagWrapper {
        return PlugPag(context)
    }
}