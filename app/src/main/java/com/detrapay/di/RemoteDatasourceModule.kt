package com.detrapay.di

import com.detrapay.data.api.DetrapayService
import com.detrapay.data.api.SupabaseService
import com.detrapay.data.api.PublicImageValidator
import com.detrapay.data.api.PublicMediaService
import com.detrapay.data.api.PublicMediaUrlPolicy
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteDatasourceModule {
    @Singleton
    @Provides
    fun provideDetrapayRemoteDataSource(
        detrapayService: DetrapayService,
        supabaseService: SupabaseService,
        publicMediaService: PublicMediaService,
        publicMediaUrlPolicy: PublicMediaUrlPolicy,
        publicImageValidator: PublicImageValidator,
    ): DetrapayRemoteDataSource {
        return DetrapayRemoteDataSource(
            detrapayService,
            supabaseService,
            publicMediaService,
            publicMediaUrlPolicy,
            publicImageValidator,
        )
    }

}
