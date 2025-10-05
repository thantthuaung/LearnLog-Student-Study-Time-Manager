package com.example.learnlog.di

import android.content.Context
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.SubjectDao
import com.example.learnlog.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideSessionLogDao(database: AppDatabase): SessionLogDao {
        return database.sessionLogDao()
    }

    @Provides
    fun provideSubjectDao(database: AppDatabase): SubjectDao {
        return database.subjectDao()
    }
}
