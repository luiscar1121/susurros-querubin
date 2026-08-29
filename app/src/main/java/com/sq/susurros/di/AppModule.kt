// app/src/main/java/com/sq/susurros/di/AppModule.kt
package com.sq.susurros.di

import android.content.ContentResolver
import android.content.Context
import androidx.hilt.annotation.ApplicationContext
import com.sq.susurros.service.AudioStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias con Hilt.
 * Provee instancias singleton para ContentResolver, Context,
 * AudioStateManager, y repositorios.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideContentResolver(context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideAudioStateManager(): AudioStateManager {
        return AudioStateManager()
    }
}
