package com.example.markdownpdfeditor.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // Skeleton Hilt providers
    /*
    @Provides
    @Singleton
    fun provideSampleService(): SampleService {
        return SampleServiceImpl()
    }
    */
}
