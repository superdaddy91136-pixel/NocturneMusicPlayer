package com.nocturne.music.di

import android.content.Context
import androidx.room.Room
import com.nocturne.music.database.MusicDao
import com.nocturne.music.database.NocturneDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNocturneDatabase(
        @ApplicationContext context: Context
    ): NocturneDatabase {
        return Room.databaseBuilder(
            context,
            NocturneDatabase::class.java,
            "nocturne_music.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMusicDao(database: NocturneDatabase): MusicDao {
        return database.musicDao()
    }
}
