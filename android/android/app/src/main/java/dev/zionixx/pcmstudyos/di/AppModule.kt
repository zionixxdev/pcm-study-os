package dev.zionixx.pcmstudyos.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.zionixx.pcmstudyos.data.local.PcmDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PcmDatabase =
        Room.databaseBuilder(context, PcmDatabase::class.java, PcmDatabase.NAME)
            .addMigrations(PcmDatabase.MIGRATION_1_2)
            .build()
}
